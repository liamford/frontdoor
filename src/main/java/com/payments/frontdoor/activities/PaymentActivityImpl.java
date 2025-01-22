package com.payments.frontdoor.activities;

import com.payments.frontdoor.exception.*;
import com.payments.frontdoor.model.PaymentAuthorizationResponse;
import com.payments.frontdoor.model.PaymentDetails;
import com.payments.frontdoor.model.PaymentInstruction;
import com.payments.frontdoor.model.PaymentOrderResponse;
import com.payments.frontdoor.service.PaymentApiConnector;
import com.payments.frontdoor.service.PaymentDispatcherService;
import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.spring.boot.ActivityImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Component
@ActivityImpl(workers = {"normal-payment-worker", "high-payment-worker"})
public class PaymentActivityImpl implements PaymentActivity {

    public static final String CORRELATION_ID_HEADER = "x-correlation-id";
    private static final String SUCCESS_STATUS = "success";
    private static final String COMPLETED_STATUS = "completed";

    private final PaymentApiConnector paymentApiConnector;
    private final PaymentDispatcherService dispatcherService;

    public PaymentActivityImpl(PaymentApiConnector paymentApiConnector,
                               PaymentDispatcherService dispatcherService) {
        this.paymentApiConnector = paymentApiConnector;
        this.dispatcherService = dispatcherService;
    }

    @Override
    public PaymentInstruction initiatePayment(PaymentDetails input) {
        log.info("Initiating payment for: {}", input);
        return convertToPaymentInstruction(input);
    }

    @Override
    public boolean managePaymentOrder(PaymentInstruction instruction) {
        return Boolean.TRUE.equals(executePaymentOperation("payment order", instruction, () -> {
            PaymentOrderResponse response = paymentApiConnector.callOrderPayment(instruction);
            validateOrderResponse(response);
            return true;
        }));
    }

    @Override
    public boolean authorizePayment(PaymentInstruction instruction) {
        return Boolean.TRUE.equals(executePaymentOperation("payment authorization", instruction, () -> {
            PaymentAuthorizationResponse response = paymentApiConnector.callAuthorizePayment(instruction);
            validateAuthorizationResponse(response);
            return true;
        }));
    }

    @Override
    public PaymentStepStatus executePayment(PaymentInstruction instruction) {
        return handlePaymentStep(instruction, PaymentStepStatus.EXECUTED, null);
    }

    @Override
    public PaymentStepStatus clearAndSettlePayment(PaymentInstruction instruction) {
        return handlePaymentStep(instruction, PaymentStepStatus.CLEARED, null);
    }

    @Override
    public PaymentStepStatus sendNotification(PaymentInstruction instruction) {
        return handlePaymentStep(instruction, PaymentStepStatus.NOTIFIED, null);
    }

    @Override
    public PaymentStepStatus reconcilePayment(PaymentInstruction instruction) {
        return handlePaymentStep(instruction, PaymentStepStatus.RECONCILED, null);
    }

    @Override
    public PaymentStepStatus postPayment(PaymentInstruction instruction) {
        validateDebtorCreditor(instruction);
        ActivityExecutionContext context = Activity.getExecutionContext();
        byte[] taskToken = context.getTaskToken();

        CompletableFuture.runAsync(() ->
                        dispatcherService.dispatchPayment(instruction, PaymentStepStatus.POSTED, taskToken),
                ForkJoinPool.commonPool());

        context.doNotCompleteOnReturn();
        return PaymentStepStatus.POSTED;
    }

    @Override
    public PaymentStepStatus generateReports(PaymentInstruction instruction) {
        return handlePaymentStep(instruction, PaymentStepStatus.REPORTED, null);
    }

    @Override
    public PaymentStepStatus archivePayment(PaymentInstruction instruction) {
        return handlePaymentStep(instruction, PaymentStepStatus.ARCHIVED, null);
    }

    @Override
    public PaymentStepStatus refundPayment(PaymentInstruction instruction) {
        return handlePaymentStep(instruction, PaymentStepStatus.REFUND, null);
    }

    private <T> T executePaymentOperation(String operationName, PaymentInstruction instruction,
                                          PaymentOperation<T> operation) {
        String correlationId = instruction.getHeaders().get(CORRELATION_ID_HEADER);
        try {
            log.info("Initiating {} for instruction: {}", operationName, instruction);
            T result = operation.execute();
            log.debug("{} successful for instruction: {}", operationName, instruction);
            return result;
        } catch (PaymentOrderFailedException | PaymentAuthorizationFailedException e) {
            logAndRethrow("Payment operation failed", correlationId, e);
        } catch (PaymentBadRequestException e) {
            logAndRethrow("Invalid payment request", correlationId, e);
        } catch (PaymentUnauthorizedException | PaymentForbiddenException e) {
            logAndRethrow("Authentication/Authorization failed", correlationId, e);
        } catch (PaymentServerException e) {
            log.error("Payment service error: [correlationId={}] - {}", correlationId, e.getMessage());
            throw new PaymentProcessingException("Payment service temporarily unavailable", e);
        } catch (Exception e) {
            log.error("Unexpected error during {}: [correlationId={}]", operationName, correlationId, e);
            throw new PaymentProcessingException("Unexpected error during payment processing", e);
        }
        return null; // This line will never be reached due to the exception handling
    }

    private PaymentStepStatus handlePaymentStep(PaymentInstruction instruction,
                                                PaymentStepStatus status, byte[] taskToken) {
        log.info("Processing payment step {} for: {}", status, instruction);
        dispatcherService.dispatchPayment(instruction, status, taskToken);
        return status;
    }

    private void validateOrderResponse(PaymentOrderResponse response) {
        if (response == null) {
            throw new PaymentOrderFailedException("Payment Order failed");
        }
        if (!COMPLETED_STATUS.equalsIgnoreCase(response.getStatus())) {
            throw new PaymentOrderFailedException(
                    String.format("Payment Order failed with status: %s", response.getStatus())
            );
        }
    }

    private void validateAuthorizationResponse(PaymentAuthorizationResponse response) {
        if (response == null) {
            throw new PaymentAuthorizationFailedException("Payment authorization failed");
        }
        if (!SUCCESS_STATUS.equalsIgnoreCase(response.getStatus())) {
            throw new PaymentAuthorizationFailedException(
                    String.format("Payment authorization failed with status: %s", response.getStatus())
            );
        }
    }

    private void validateDebtorCreditor(PaymentInstruction instruction) {
        if (instruction.getDebtor().equals(instruction.getCreditor())) {
            throw new IllegalArgumentException("Debtor and creditor accounts cannot be the same");
        }
    }

    private void logAndRethrow(String message, String correlationId, Exception e) {
        log.error("{}: [correlationId={}] - {}", message, correlationId, e.getMessage());
        throw e instanceof RuntimeException ? (RuntimeException) e :
                new PaymentProcessingException(message, e);
    }

    private PaymentInstruction convertToPaymentInstruction(PaymentDetails details) {
        return PaymentInstruction.builder()
                .paymentStatus(details.getPaymentStatus())
                .paymentId(details.getPaymentId())
                .debtor(details.getDebtor())
                .creditor(details.getCreditor())
                .amount(details.getAmount())
                .currency(details.getCurrency())
                .paymentReference(details.getPaymentReference())
                .paymentDate(details.getPaymentDate())
                .bankAddress("Main Road")
                .bankCountry("Australia")
                .bankCity("Melbourne")
                .bic("LIAM123")
                .bankName("Liam Bank")
                .headers(details.getHeaders())
                .build();
    }


    @FunctionalInterface
    private interface PaymentOperation<T> {
        T execute() throws Exception;
    }
}

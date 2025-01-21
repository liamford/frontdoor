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

import java.util.concurrent.ForkJoinPool;

@Slf4j
@Component
@ActivityImpl(workers = "send-payment-worker")
public class PaymentActivityImpl implements PaymentActivity {


    public static final String CORRELATION_ID_HEADER = "x-correlation-id";
    private static final String SUCCESS_STATUS = "success";
    private static final String COMPLETED_STATUS = "completed";
    private final PaymentApiConnector paymentApiConnector;
    private final PaymentDispatcherService dispatcherService;


    public PaymentActivityImpl(PaymentApiConnector paymentApiConnector, PaymentDispatcherService dispatcherService) {
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
        log.info("Managing payment order for: {}", instruction);
        String correlationId = instruction.getHeaders().get(CORRELATION_ID_HEADER);
        try {
            log.info("Initiating payment Order for instruction: {}", instruction);
            PaymentOrderResponse response = paymentApiConnector.callOrderPayment(instruction);
            validateOrderResponse(response);
            log.debug("Payment Order successful for instruction: {}", instruction);
            return true;
        }
        catch (PaymentOrderFailedException e) {
            log.error("Payment Authorization failed: [correlationId={}] - {}",
                    correlationId,
                    e.getMessage());
            throw e;

        }
        catch (PaymentBadRequestException e) {
            log.error("Invalid payment request: [correlationId={}] - {}",
                    correlationId,
                    e.getMessage());
            throw e;

        } catch (PaymentUnauthorizedException | PaymentForbiddenException e) {
            log.error("Authentication/Authorization failed: [correlationId={}] - {}",
                    correlationId,
                    e.getMessage());
            throw e;

        } catch (PaymentServerException e) {
            log.error("Payment service error: [correlationId={}] - {}",
                    correlationId,
                    e.getMessage());
            throw new PaymentProcessingException("Payment service temporarily unavailable", e);

        } catch (Exception e) {
            log.error("Unexpected error during payment authorization: [correlationId={}]",
                    correlationId,
                    e);
            throw new PaymentProcessingException("Unexpected error during payment processing", e);
        }
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

    @Override
    public boolean authorizePayment(PaymentInstruction instruction) {
        String correlationId = instruction.getHeaders().get(CORRELATION_ID_HEADER);
        try {
            log.info("Initiating payment authorization for instruction: {}", instruction);

            PaymentAuthorizationResponse response = paymentApiConnector.callAuthorizePayment(instruction);
            validateAuthorizationResponse(response);
            log.debug("Payment authorization successful for instruction: {}", instruction);
            return true;

        }
        catch (PaymentAuthorizationFailedException e) {
            log.error("Payment Authorization failed: [correlationId={}] - {}",
                    correlationId,
                    e.getMessage());
            throw e;

        }
        catch (PaymentBadRequestException e) {
            log.error("Invalid payment request: [correlationId={}] - {}",
                    correlationId,
                    e.getMessage());
            throw e;

        } catch (PaymentUnauthorizedException | PaymentForbiddenException e) {
            log.error("Authentication/Authorization failed: [correlationId={}] - {}",
                    correlationId,
                    e.getMessage());
            throw e;

        } catch (PaymentServerException e) {
            log.error("Payment service error: [correlationId={}] - {}",
                    correlationId,
                    e.getMessage());
            throw new PaymentProcessingException("Payment service temporarily unavailable", e);

        } catch (Exception e) {
            log.error("Unexpected error during payment authorization: [correlationId={}]",
                    correlationId,
                    e);
            throw new PaymentProcessingException("Unexpected error during payment processing", e);
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

    @Override
    public PaymentStepStatus executePayment(PaymentInstruction instruction) {
        log.info("Executing payment: {}", instruction);
        dispatcherService.dispatchPayment(instruction, PaymentStepStatus.EXECUTED, null);
        return PaymentStepStatus.EXECUTED;
    }

    @Override
    public PaymentStepStatus clearAndSettlePayment(PaymentInstruction instruction) {
        log.info("Clearing and settling payment: {}", instruction);
        dispatcherService.dispatchPayment(instruction, PaymentStepStatus.CLEARED, null);

        return PaymentStepStatus.CLEARED;
    }

    @Override
    public PaymentStepStatus sendNotification(PaymentInstruction instruction) {
        log.info("Sending notification for payment: {}", instruction);
        dispatcherService.dispatchPayment(instruction, PaymentStepStatus.NOTIFIED, null);
        return PaymentStepStatus.NOTIFIED;
    }

    @Override
    public PaymentStepStatus reconcilePayment(PaymentInstruction instruction) {
        log.info("Reconciling payment: {}", instruction);
        dispatcherService.dispatchPayment(instruction, PaymentStepStatus.RECONCILED, null);
        return PaymentStepStatus.RECONCILED;
    }

    @Override
    public PaymentStepStatus postPayment(PaymentInstruction instruction) {
        log.info("Posting payment to ledger: {}", instruction);
        if (instruction.getDebtor().equals(instruction.getCreditor())) {
            throw new IllegalArgumentException("Debtor and creditor accounts are same");
        }
        ActivityExecutionContext context = Activity.getExecutionContext();
        byte[] taskToken = context.getTaskToken();

        ForkJoinPool.commonPool().execute(() ->
                dispatcherService.dispatchPayment(instruction, PaymentStepStatus.POSTED, taskToken));
        context.doNotCompleteOnReturn();
        return PaymentStepStatus.POSTED;
    }

    @Override
    public PaymentStepStatus generateReports(PaymentInstruction instruction) {
        log.info("Generating reports for payment: {}", instruction);
        dispatcherService.dispatchPayment(instruction, PaymentStepStatus.REPORTED, null);

        return PaymentStepStatus.REPORTED;
    }

    @Override
    public PaymentStepStatus archivePayment(PaymentInstruction instruction) {
        log.info("Archiving payment: {}", instruction);
        dispatcherService.dispatchPayment(instruction, PaymentStepStatus.ARCHIVED, null);

        return PaymentStepStatus.ARCHIVED;
    }

    @Override
    public PaymentStepStatus refundPayment(PaymentInstruction instruction) {
        log.info("Refunding payment: {}", instruction);
        dispatcherService.dispatchPayment(instruction, PaymentStepStatus.REFUND, null);

        return PaymentStepStatus.REFUND;
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
}
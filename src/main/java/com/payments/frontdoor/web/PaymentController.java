package com.payments.frontdoor.web;

import com.payments.frontdoor.exception.IdempotencyKeyMismatchException;
import com.payments.frontdoor.exception.PaymentValidationException;
import com.payments.frontdoor.model.*;
import com.payments.frontdoor.service.PaymentProcessService;
import com.payments.frontdoor.swagger.model.CrossBorderPaymentRequest;
import com.payments.frontdoor.swagger.model.PaymentRequest;
import com.payments.frontdoor.swagger.model.PaymentResponse;
import com.payments.frontdoor.swagger.model.PaymentStatusResponse;
import com.payments.frontdoor.util.PaymentUtil;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static com.payments.frontdoor.util.PaymentUtil.*;

@Slf4j
@RestController
@lombok.AllArgsConstructor
public class PaymentController {

    private final PaymentProcessService paymentProcessService;

    private static final Duration POLLING_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration POLLING_INTERVAL = Duration.ofMillis(100);

    @PostMapping(value = "/submit-payment",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentResponse> submitPayment(
            @RequestHeader(PaymentHeaders.CORRELATION_ID) String correlationId,
            @RequestHeader(PaymentHeaders.IDEMPOTENCY_KEY) String idempotencyKey,
            @RequestHeader(PaymentHeaders.REQUEST_STATUS) String requestStatus,
            @Valid @RequestBody PaymentRequest request,
            BindingResult bindingResult) {

        logPaymentRequest(request, correlationId, PaymentType.NORMAL);
        validateRequest(bindingResult, idempotencyKey, request.getPaymentReference());
        String uetr = generateUetr();
        PaymentDetails paymentDetails = createPaymentDetails(request, uetr, correlationId,
                idempotencyKey, requestStatus);

        return processPaymentRequest(paymentDetails, uetr, requestStatus);

    }

    @PostMapping(value = "/cross-border-payment",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentResponse> submitCrossBoarderPayment(
            @RequestHeader(PaymentHeaders.CORRELATION_ID) String correlationId,
            @RequestHeader(PaymentHeaders.IDEMPOTENCY_KEY) String idempotencyKey,
            @Valid @RequestBody CrossBorderPaymentRequest request,
            BindingResult bindingResult) {

        logPaymentRequest(request, correlationId, PaymentType.CROSS_BOARDER);
        validateRequest(bindingResult, idempotencyKey, request.getPaymentReference());
        String uetr = generateUetr();
        CrossBoarderPaymentDetails crossBorderPaymentRequest = createCrossBoarderPaymentDetails(request, uetr, correlationId,
               idempotencyKey);
        return processCrossPaymentRequest(crossBorderPaymentRequest, uetr);

    }


    @GetMapping(value = "/payment-status/{paymentId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @RequestHeader(PaymentHeaders.CORRELATION_ID) String correlationId,
            @PathVariable String paymentId,
            @RequestParam(name = "includeActivities", required = false, defaultValue = "false") boolean includeActivities) {

        log.info("Received request to get payment status for paymentId: {} - correlationId: {}", paymentId, correlationId);

        WorkflowResult workflowResult = paymentProcessService.retrieveWorkFlowHistory(paymentId, includeActivities);
        PaymentStatusResponse response = convertToPaymentStatusResponse(workflowResult, paymentId);
        response.setPaymentId(paymentId);

        return ResponseEntity.ok(response);
    }


    @UtilityClass
    class PaymentHeaders {
        public static final String CORRELATION_ID = "x-correlation-id";
        public static final String IDEMPOTENCY_KEY = "x-idempotency-key";
        public static final String REQUEST_STATUS = "x-request-status";
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public static class PaymentProcessingException extends RuntimeException {
        public PaymentProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }


    private  <T> void logPaymentRequest(T request, String correlationId, PaymentType paymentType) {
        String reference = getPaymentReference(request);
        log.info("Received payment {} with request: {} - correlationId: {}",
                paymentType.name(),
                reference,
                correlationId);
    }




    private void validateRequest(BindingResult bindingResult, String idempotencyKey,
                                 String paymentReference) {
        if (bindingResult.hasErrors()) {
            log.error("Payment request validation failed");
            throw new PaymentValidationException("Validation error");
        }
        validateIdempotencyKey(idempotencyKey, paymentReference);
    }

    private void validateIdempotencyKey(String idempotencyKey, String paymentReference) {
        if (!idempotencyKey.equals(paymentReference)) {
            throw new IdempotencyKeyMismatchException("Idempotency key does not match payment reference");
        }
    }

    private PaymentDetails createPaymentDetails(PaymentRequest request, String uetr,
                                                String correlationId, String idempotencyKey, String requestStatus) {

        Map<String, String> headers = Map.of(
                PaymentHeaders.CORRELATION_ID, correlationId,
                PaymentHeaders.IDEMPOTENCY_KEY, idempotencyKey,
                PaymentHeaders.REQUEST_STATUS, requestStatus
        );

        return (PaymentDetails) getDetails(request, uetr, headers);
    }

    private CrossBoarderPaymentDetails createCrossBoarderPaymentDetails(CrossBorderPaymentRequest request, String uetr,
                                                                        String correlationId, String idempotencyKey) {

        Map<String, String> headers = Map.of(
                PaymentHeaders.CORRELATION_ID, correlationId,
                PaymentHeaders.IDEMPOTENCY_KEY, idempotencyKey
        );

        return (CrossBoarderPaymentDetails) getDetails(request, uetr, headers);
    }

    private ResponseEntity<PaymentResponse> processPaymentRequest(PaymentDetails paymentDetails,
                                                                  String uetr, String requestStatus) {

        paymentProcessService.processPaymentAsync(paymentDetails);

        return PaymentStatus.SYNC.getCode().equals(requestStatus)
                ? handleSyncPayment(uetr)
                : handleAsyncPayment(uetr);
    }

    private ResponseEntity<PaymentResponse> processCrossPaymentRequest(CrossBoarderPaymentDetails paymentDetails,
                                                                  String uetr) {

        paymentProcessService.processCrossBoarderPaymentAsync(paymentDetails);
        return handleAsyncPayment(uetr);
    }

    private ResponseEntity<PaymentResponse> handleSyncPayment(String uetr) {
        try {
            WorkflowExecutionStatus workflowStatus = pollUntilWorkflowComplete(uetr
            );
            PaymentResponse response = PaymentUtil.createPaymentResponse(uetr, workflowStatus);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing payment for UETR: {}", uetr, e);
            throw new PaymentProcessingException("Payment processing failed", e);
        }
    }

    private ResponseEntity<PaymentResponse> handleAsyncPayment(String uetr) {
        PaymentResponse response = PaymentUtil.createPaymentResponse(uetr,
                PaymentResponse.StatusEnum.ACTC);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private WorkflowExecutionStatus pollUntilWorkflowComplete(String uetr){
        Instant startTime = Instant.now();

        while (true) {
            WorkflowExecutionStatus status = paymentProcessService.getWorkflowStatus(uetr);

            // If status is not RUNNING, return the final status
            if (status != WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING) {
                return status;
            }

            // Check if we've exceeded the timeout
            if (Duration.between(startTime, Instant.now()).compareTo(PaymentController.POLLING_TIMEOUT) > 0) {
                return status;
            }

            try {
                Thread.sleep(PaymentController.POLLING_INTERVAL.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new PaymentProcessingException("Polling was interrupted", e);
            }
        }
    }

}

package com.payments.frontdoor.web;

import com.payments.frontdoor.exception.IdempotencyKeyMismatchException;
import com.payments.frontdoor.exception.PaymentValidationException;
import com.payments.frontdoor.model.PaymentDetails;
import com.payments.frontdoor.model.PaymentPriority;
import com.payments.frontdoor.model.PaymentStatus;
import com.payments.frontdoor.model.WorkflowResult;
import com.payments.frontdoor.service.PaymentProcessService;
import com.payments.frontdoor.swagger.model.Activities;
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

        logPaymentRequest(request, correlationId);
        validateRequest(bindingResult, idempotencyKey, request.getPaymentReference());
        String uetr = generateUetr();
        PaymentDetails paymentDetails = createPaymentDetails(request, uetr, correlationId,
                idempotencyKey, requestStatus);

        return processPaymentRequest(paymentDetails, uetr, requestStatus);

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
    public class PaymentProcessingException extends RuntimeException {
        public PaymentProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }


    private void logPaymentRequest(PaymentRequest request, String correlationId) {
        log.info("Received payment request: {} - correlationId: {}",
                request.getPaymentReference(), correlationId);
    }

    private void validateRequest(BindingResult bindingResult, String idempotencyKey,
                                 String paymentReference) {
        if (bindingResult.hasErrors()) {
            log.error("Payment request validation failed");
            throw new PaymentValidationException("Validation error");
        }
        validateIdempotencyKey(idempotencyKey, paymentReference);
    }

    private String generateUetr() {
        return UUID.randomUUID().toString();
    }

    private PaymentDetails createPaymentDetails(PaymentRequest request, String uetr,
                                                String correlationId, String idempotencyKey, String requestStatus) {

        Map<String, String> headers = Map.of(
                PaymentHeaders.CORRELATION_ID, correlationId,
                PaymentHeaders.IDEMPOTENCY_KEY, idempotencyKey,
                PaymentHeaders.REQUEST_STATUS, requestStatus
        );

        return getPaymentDetails(request, uetr, headers);
    }

    private ResponseEntity<PaymentResponse> processPaymentRequest(PaymentDetails paymentDetails,
                                                                  String uetr, String requestStatus) {

        paymentProcessService.processPaymentAsync(paymentDetails);

        return PaymentStatus.SYNC.getCode().equals(requestStatus)
                ? handleSyncPayment(uetr)
                : handleAsyncPayment(uetr);
    }

    private ResponseEntity<PaymentResponse> handleSyncPayment(String uetr) {
        try {
            WorkflowExecutionStatus workflowStatus = pollUntilWorkflowComplete(uetr,
                    POLLING_TIMEOUT, POLLING_INTERVAL);
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



    private PaymentStatusResponse convertToPaymentStatusResponse(WorkflowResult workflowResult, String paymentId) {
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setPaymentId(paymentId);
        response.setWorkflow(workflowResult.getWorkflowType());
        response.setStartTime(PaymentUtil.convertToOffsetDateTime(workflowResult.getStartTime()));
        response.setEndTime(PaymentUtil.convertToOffsetDateTime(workflowResult.getEndTime()));
        switch (workflowResult.getWorkflowStatus()) {
            case WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED:
                response.setStatus(PaymentStatusResponse.StatusEnum.ACSC);
                break;
            case WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_FAILED:
                response.setStatus(PaymentStatusResponse.StatusEnum.RJCT);
                break;
            default:
                response.setStatus(PaymentStatusResponse.StatusEnum.ACTC);
                break;
        }

        if (workflowResult.getActivities() != null) {
            response.setActivities(workflowResult.getActivities().stream()
                    .map(activity -> {
                        Activities activityResponse = new Activities();
                        activityResponse.setActivityName(activity.getActivityName());
                        activityResponse.setStatus(activity.getStatus());
                        activityResponse.setStartTime(PaymentUtil.convertToOffsetDateTime(activity.getStartTime()));
                        return activityResponse;
                    })
                    .collect(Collectors.toList()));
        }

        return response;
    }

    private WorkflowExecutionStatus pollUntilWorkflowComplete(String uetr, Duration timeout, Duration pollInterval){
        Instant startTime = Instant.now();

        while (true) {
            WorkflowExecutionStatus status = paymentProcessService.getWorkflowStatus(uetr);

            // If status is not RUNNING, return the final status
            if (status != WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING) {
                return status;
            }

            // Check if we've exceeded the timeout
            if (Duration.between(startTime, Instant.now()).compareTo(timeout) > 0) {
                return status;
            }

            try {
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Polling was interrupted", e);
            }
        }
    }

    private PaymentDetails getPaymentDetails(PaymentRequest request, String uetr, Map<String, String> headers) {
        return PaymentDetails.builder()
                .paymentStatus(PaymentResponse.StatusEnum.ACTC.toString())
                .paymentId(uetr)
                .debtor(request.getDebtor())
                .creditor(request.getCreditor())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentReference(request.getPaymentReference())
                .paymentDate(request.getPaymentDate())
                .priority(PaymentPriority.valueOf(Optional.ofNullable(request.getPriority())
                        .map(Enum::toString)
                        .orElse("NORMAL")))
                .headers(headers)
                .build();
    }

    private void validateIdempotencyKey(String idempotencyKey, String paymentReference) {
        if (!idempotencyKey.equals(paymentReference)) {
            throw new IdempotencyKeyMismatchException("Idempotency key does not match payment reference");
        }
    }

}
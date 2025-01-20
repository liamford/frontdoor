package com.payments.frontdoor.web;

import com.payments.frontdoor.exception.IdempotencyKeyMismatchException;
import com.payments.frontdoor.exception.PaymentValidationException;
import com.payments.frontdoor.model.PaymentDetails;
import com.payments.frontdoor.model.WorkflowResult;
import com.payments.frontdoor.service.PaymentProcessService;
import com.payments.frontdoor.swagger.model.Activities;
import com.payments.frontdoor.swagger.model.PaymentRequest;
import com.payments.frontdoor.swagger.model.PaymentResponse;
import com.payments.frontdoor.swagger.model.PaymentStatusResponse;
import com.payments.frontdoor.util.PaymentUtil;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@lombok.AllArgsConstructor
public class PaymentController {

    private final PaymentProcessService paymentProcessService;

    @PostMapping(value = "/submit-payment", consumes = "application/json", produces = "application/json")
    public ResponseEntity<PaymentResponse> payment(
            @RequestHeader(name = "x-correlation-id") String correlationId,
            @RequestHeader(name = "x-idempotency-key") String idempotencyKey,
            final @RequestBody @Valid PaymentRequest request,
            BindingResult bindingResult) {

        log.info("Received payment request: {} - correlationId: {}", request.getPaymentReference(), correlationId);

        if (bindingResult.hasErrors()) {
            throw new PaymentValidationException("Validation error");
        }

        validateIdempotencyKey(idempotencyKey, request.getPaymentReference());
        String uetr = UUID.randomUUID().toString();
        Map<String, String> headers = new HashMap<>();

        headers.put("x-correlation-id", correlationId);
        headers.put("x-idempotency-key", idempotencyKey);

        PaymentDetails paymentDetails = getPaymentDetails(request, uetr , headers);

        paymentProcessService.processPaymentAsync(paymentDetails);

        PaymentResponse response = PaymentUtil.createPaymentResponse(uetr, PaymentResponse.StatusEnum.ACTC);

        log.info("Payment Created with paymentId: {}", response.getPaymentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/payment-status/{paymentId}", produces = "application/json")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @RequestHeader(name = "x-correlation-id") String correlationId,
            @PathVariable String paymentId,
            @RequestParam(name = "includeActivities", required = false, defaultValue = "false") boolean includeActivities) {

        log.info("Received request to get payment status for paymentId: {} - correlationId: {}", paymentId, correlationId);

        WorkflowResult workflowResult = paymentProcessService.retrieveWorkFlowHistory(paymentId, includeActivities);
        PaymentStatusResponse response = convertToPaymentStatusResponse(workflowResult, paymentId);
        response.setPaymentId(paymentId);

        return ResponseEntity.ok(response);
    }

    private PaymentStatusResponse convertToPaymentStatusResponse(WorkflowResult workflowResult, String paymentId) {
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setPaymentId(paymentId);
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
                .headers(headers)
                .build();
    }

    private void validateIdempotencyKey(String idempotencyKey, String paymentReference) {
        if (!idempotencyKey.equals(paymentReference)) {
            throw new IdempotencyKeyMismatchException("Idempotency key does not match payment reference");
        }
    }

}
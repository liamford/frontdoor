package com.payments.frontdoor.web;

import com.payments.frontdoor.util.PaymentUtil;
import com.payments.frontdoor.exception.IdempotencyKeyMismatchException;
import com.payments.frontdoor.exception.PaymentValidationException;
import com.payments.frontdoor.service.PaymentProcessService;
import com.payments.frontdoor.swagger.model.PaymentRequest;
import com.payments.frontdoor.swagger.model.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import model.PaymentDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

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

        PaymentDetails paymentDetails = getPaymentDetails(request, uetr);

        String workflowId = request.getPaymentReference();
        paymentProcessService.processPaymentAsync(paymentDetails, workflowId);

        PaymentResponse response = PaymentUtil.createPaymentResponse(uetr, PaymentResponse.StatusEnum.ACTC);

        log.info("Payment Created with paymentId: {}", response.getPaymentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private PaymentDetails getPaymentDetails(PaymentRequest request, String uetr) {
        return PaymentDetails.builder()
                .paymentStatus(PaymentResponse.StatusEnum.ACTC.toString())
                .paymentId(uetr)
                .debtor(request.getDebtor())
                .creditor(request.getCreditor())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentReference(request.getPaymentReference())
                .paymentDate(request.getPaymentDate())
                .build();
    }

    private void validateIdempotencyKey(String idempotencyKey, String paymentReference) {
        if (!idempotencyKey.equals(paymentReference)) {
            throw new IdempotencyKeyMismatchException("Idempotency key does not match payment reference");
        }
    }

}
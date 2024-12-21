package com.payments.frontdoor.web;

import com.payments.frontdoor.exception.IdempotencyKeyMismatchException;
import com.payments.frontdoor.exception.PaymentValidationException;
import com.payments.frontdoor.swagger.model.PaymentRequest;
import com.payments.frontdoor.swagger.model.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
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
public class PaymentController {

    @PostMapping(value = "/submit-payment", consumes = "application/json")
    public ResponseEntity<PaymentResponse> payment(
            @RequestHeader(name = "x-correlation-id") String correlationId,
            @RequestHeader(name = "x-idempotency-key") String idempotencyKey,
            final @RequestBody @Valid PaymentRequest request,
            BindingResult bindingResult) {

        log.info("Received payment request: {} - correlationId: {}", request, correlationId);

        if (bindingResult.hasErrors()) {
            throw new PaymentValidationException("Validation error");
        }


        validateIdempotencyKey(idempotencyKey, request.getPaymentReference());
        PaymentResponse response = createPaymentResponse();

        log.info("Payment Created with paymentId: {}", response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private void validateIdempotencyKey(String idempotencyKey, String paymentReference) {
        if (!idempotencyKey.equals(paymentReference)) {
            throw new IdempotencyKeyMismatchException("Idempotency key does not match payment reference");
        }
    }

    private PaymentResponse createPaymentResponse() {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(UUID.randomUUID().toString());
        response.setStatus(PaymentResponse.StatusEnum.ACTC);
        return response;
    }
}
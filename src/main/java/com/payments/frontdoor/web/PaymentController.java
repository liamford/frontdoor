package com.payments.frontdoor.web;


import com.payments.frontdoor.exception.IdempotencyKeyMismatchException;
import com.payments.frontdoor.exception.IdempotencyMissingException;
import com.payments.frontdoor.exception.PaymentValidationException;
import com.payments.frontdoor.swagger.model.PaymentRequest;
import com.payments.frontdoor.swagger.model.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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
    public ResponseEntity<PaymentResponse> payment(@RequestHeader HttpHeaders headers,
                                                   final @RequestBody @Valid PaymentRequest request,
                                                   BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new PaymentValidationException("Validation error");
        }

        String correlationId = headers.getFirst("x-correlation-id");
        String idempotencyKey = headers.getFirst("x-idempotency-key");
        log.info("Payment request received with payment reference: {} - correlationId: {}",
                request.getPaymentReference()
                , correlationId);

        validateIdempotencyKey(idempotencyKey, request.getPaymentReference());
        PaymentResponse response = createPaymentResponse();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private void validateIdempotencyKey(String idempotencyKey, String paymentReference) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            throw new IdempotencyMissingException("Idempotency key is missing or empty");
        }
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

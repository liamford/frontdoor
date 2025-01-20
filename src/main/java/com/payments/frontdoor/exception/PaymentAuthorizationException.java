package com.payments.frontdoor.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
public class PaymentAuthorizationException extends RuntimeException {
    public PaymentAuthorizationException(String message) {
        super(message);
        log.error("Payment authorization error");
    }
    public PaymentAuthorizationException(String message, Throwable cause) {
        super(message, cause);
        log.error("Payment authorization error: {}", message, cause);
    }
}
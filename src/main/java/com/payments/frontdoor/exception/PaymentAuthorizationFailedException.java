package com.payments.frontdoor.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentAuthorizationFailedException extends RuntimeException {
    public PaymentAuthorizationFailedException(String message) {
        super(message);
        log.error("Payment authorization error");
    }
    public PaymentAuthorizationFailedException(String message, Throwable cause) {
        super(message, cause);
        log.error("Payment authorization error: {}", message, cause);
    }
}
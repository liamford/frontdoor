package com.payments.frontdoor.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentOrderFailedException extends RuntimeException {
    public PaymentOrderFailedException(String message) {
        super(message);
        log.error("Payment Order error");
    }
    public PaymentOrderFailedException(String message, Throwable cause) {
        super(message, cause);
        log.error("Payment Order error: {}", message, cause);
    }
}
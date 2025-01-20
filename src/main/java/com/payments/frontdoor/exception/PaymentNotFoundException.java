package com.payments.frontdoor.exception;


import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message) {
        super(message);
        log.error("Payment Bad Request error");
    }

    public PaymentNotFoundException(String message, Throwable cause) {
        super(message, cause);
        log.error("Payment Bad Request error: {}", message, cause);
    }
}

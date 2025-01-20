package com.payments.frontdoor.exception;


import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentForbiddenException extends RuntimeException {
    public PaymentForbiddenException(String message) {
        super(message);
        log.error("Payment Bad Request error");
    }

    public PaymentForbiddenException(String message, Throwable cause) {
        super(message, cause);
        log.error("Payment Bad Request error: {}", message, cause);
    }
}

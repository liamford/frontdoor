package com.payments.frontdoor.exception;


import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentGatewayException extends RuntimeException {
    public PaymentGatewayException(String message) {
        super(message);
        log.error("Payment Bad Request error");
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
        log.error("Payment Bad Request error: {}", message, cause);
    }
}

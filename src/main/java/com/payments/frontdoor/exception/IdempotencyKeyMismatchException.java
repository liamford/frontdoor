package com.payments.frontdoor.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class IdempotencyKeyMismatchException extends RuntimeException {
    public IdempotencyKeyMismatchException(String message) {
        super(message);
    }
}

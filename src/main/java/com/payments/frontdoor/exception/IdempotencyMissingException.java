package com.payments.frontdoor.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class IdempotencyMissingException extends RuntimeException {
    public IdempotencyMissingException(String message) {
        super(message);
    }
}

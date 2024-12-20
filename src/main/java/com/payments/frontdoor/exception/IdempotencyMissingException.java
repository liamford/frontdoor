package com.payments.frontdoor.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
@Slf4j
public class IdempotencyMissingException extends RuntimeException {
    public IdempotencyMissingException(String message) {
        super(message);
        log.error("Idempotency key is missing or empty");
    }
}

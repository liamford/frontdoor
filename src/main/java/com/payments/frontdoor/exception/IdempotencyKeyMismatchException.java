package com.payments.frontdoor.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
@Slf4j
public class IdempotencyKeyMismatchException extends RuntimeException {
    public IdempotencyKeyMismatchException(String message) {
        super(message);
        log.error("Idempotency key does not match payment reference");
    }
}

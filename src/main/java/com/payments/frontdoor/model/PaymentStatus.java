package com.payments.frontdoor.model;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PaymentStatus {
    SYNC("200", HttpStatus.OK),
    ASYNC("201", HttpStatus.CREATED);

    private final String code;
    private final HttpStatus httpStatus;

    PaymentStatus(String code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
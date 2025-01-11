package com.payments.frontdoor.util;

import com.payments.frontdoor.swagger.model.PaymentResponse;

import java.time.Instant;
import java.time.OffsetDateTime;

import static java.time.ZoneId.systemDefault;


public class PaymentUtil {

    public static PaymentResponse createPaymentResponse(String uetr, PaymentResponse.StatusEnum status) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(uetr);
        response.setStatus(status);
        return response;
    }

    public static OffsetDateTime convertToOffsetDateTime(com.google.protobuf.Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos())
                .atOffset(systemDefault().getRules().getOffset(Instant.now()));
    }


}

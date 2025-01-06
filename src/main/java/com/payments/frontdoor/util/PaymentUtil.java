package com.payments.frontdoor.util;

import com.payments.frontdoor.swagger.model.PaymentResponse;

public class PaymentUtil {

    public static PaymentResponse createPaymentResponse(String uetr, PaymentResponse.StatusEnum status) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(uetr);
        response.setStatus(status);
        return response;
    }

}

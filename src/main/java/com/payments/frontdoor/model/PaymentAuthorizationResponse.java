package com.payments.frontdoor.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class PaymentAuthorizationResponse {
    private String paymentId;
    private String status;
    private String authorizationCode;
    private String description;
    private LocalDateTime createdAt;
    private Map<String, String> metadata;
}

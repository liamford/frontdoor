package com.payments.frontdoor.model;

import java.util.Map;

public record RequestHeaders(String correlationId, String idempotencyKey, String requestStatus) {
    public Map<String, String> toMap() {
        return Map.of(
                "x-correlation-id", correlationId,
                "x-idempotency-key", idempotencyKey,
                "x-request-status", requestStatus
        );
    }
}

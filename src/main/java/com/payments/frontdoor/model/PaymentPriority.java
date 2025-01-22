package com.payments.frontdoor.model;

public enum PaymentPriority {
    HIGH, NORMAL, LOW;

    public static PaymentPriority fromString(String priority) {
        try {
            return valueOf(priority.toUpperCase());
        } catch (Exception e) {
            return NORMAL; // default priority
        }
    }
}


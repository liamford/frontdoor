package com.payments.frontdoor.model;

public enum PaymentType {
     NORMAL, CROSS_BOARDER;

    public static PaymentType fromString(String priority) {
        try {
            return valueOf(priority.toUpperCase());
        } catch (Exception e) {
            return NORMAL; // default priority
        }
    }
}


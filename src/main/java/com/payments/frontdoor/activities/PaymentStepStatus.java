package com.payments.frontdoor.activities;

import lombok.Getter;

@Getter
public enum PaymentStepStatus {
    INITIATED("Initiated"),
    MANAGED("Managed"),
    AUTHORIZED("Authorized"),
    EXECUTED("Executed"),
    CLEARED("Cleared"),
    NOTIFIED("Notified"),
    RECONCILED("Reconciled"),
    POSTED("Posted"),
    REFUND("Refund"),
    REPORTED("Reported"),
    ARCHIVED("Archived");

    private final String status;

    PaymentStepStatus(String status) {
        this.status = status;
    }

}
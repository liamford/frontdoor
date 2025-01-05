package com.payments.frontdoor.activities;

import lombok.Getter;

@Getter
public enum ActivityStepStatus {
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

    ActivityStepStatus(String status) {
        this.status = status;
    }

}
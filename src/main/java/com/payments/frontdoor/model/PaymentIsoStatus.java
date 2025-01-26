package com.payments.frontdoor.model;

import lombok.Getter;

@Getter
public enum PaymentIsoStatus {

    ACTC, // Accepted Technical Validation
    ACSC, // Accepted Settlement Completed
    RJCT, // Rejected
    PDNG, // Pending
    ACCP, // Accepted Customer Profile
    PART, // Partially Accepted
    CANC; // Cancelled

}

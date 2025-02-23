package com.payments.frontdoor.model;

import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class TransactionDetails {
    private String transactionId;
    private String settlementDate;
    private String clearingSystem;
}

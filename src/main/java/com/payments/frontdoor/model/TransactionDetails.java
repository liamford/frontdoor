package com.payments.frontdoor.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class TransactionDetails {
    private String transactionId;
    private String settlementDate;
    private String clearingSystem;
}

package com.payments.frontdoor.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentOrderResponse {
    private String paymentOrderId;
    private String status;
    private String statusDetails;
    private String reference;
    private LocalDateTime processingTimestamp;
    private TransactionDetails transactionDetails;
}

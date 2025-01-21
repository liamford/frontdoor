package com.payments.frontdoor.model;

import com.payments.frontdoor.swagger.model.Account;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PaymentOrderRequest {
    private String paymentId;
    private Account debtor;
    private Account creditor;
    private double amount;
    private String currency;
    private String paymentReference;
    private LocalDate paymentDate;
    private String priority;
}

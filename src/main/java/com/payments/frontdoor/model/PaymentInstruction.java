package com.payments.frontdoor.model;


import com.payments.frontdoor.swagger.model.Account;

import java.math.BigDecimal;
import java.time.LocalDate;

@lombok.Data
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Builder
public class PaymentInstruction {
    private String paymentStatus;
    private String paymentId;
    private Account debtor;
    private Account creditor;
    private BigDecimal amount;
    private String currency;
    private String paymentReference;
    private LocalDate paymentDate;
    private String bic;
    private String bankName;
    private String bankAddress;
    private String bankCity;
    private String bankCountry;
}

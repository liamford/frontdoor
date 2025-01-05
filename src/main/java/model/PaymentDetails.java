package model;


import com.payments.frontdoor.swagger.model.Account;

import java.math.BigDecimal;
import java.time.LocalDate;


@lombok.Data
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Builder
public class PaymentDetails {

    private String paymentStatus;
    private String paymentId;
    private Account debtor;
    private Account creditor;
    private BigDecimal amount;
    private String currency;
    private String paymentReference;
    private LocalDate paymentDate;
}

package com.payments.frontdoor.model;


import com.payments.frontdoor.swagger.model.CrossBorderPaymentRequestBeneficiary;
import com.payments.frontdoor.swagger.model.CrossBorderPaymentRequestCustomer;
import com.payments.frontdoor.swagger.model.CrossBorderPaymentRequestFees;
import com.payments.frontdoor.swagger.model.CrossBorderPaymentRequestTransactionDetails;

import java.util.Map;


@lombok.Data
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Builder
public class CrossBoarderPaymentDetails {

    private String paymentStatus;
    private String paymentId;
    private CrossBorderPaymentRequestCustomer customer;
    private CrossBorderPaymentRequestBeneficiary beneficiary;
    private CrossBorderPaymentRequestTransactionDetails transactionDetails;
    private CrossBorderPaymentRequestFees fees;
    private String paymentReference;
    private Map<String, String> headers;
}

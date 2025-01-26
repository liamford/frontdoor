package com.payments.frontdoor.activities;

import com.payments.frontdoor.model.CrossBoarderPaymentDetails;
import com.payments.frontdoor.model.PaymentIsoStatus;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface CrossBoarderPaymentActivity {

    @ActivityMethod
    PaymentIsoStatus debitAccount(CrossBoarderPaymentDetails input);

    @ActivityMethod
    PaymentIsoStatus debitCompensation(CrossBoarderPaymentDetails input); // Reverse debit

    @ActivityMethod
    PaymentIsoStatus reserveCurrency(CrossBoarderPaymentDetails input);

    @ActivityMethod
    PaymentIsoStatus releaseCurrency(CrossBoarderPaymentDetails input); // Release reserved currency

    @ActivityMethod
    PaymentIsoStatus performSanctionsCheck(CrossBoarderPaymentDetails input);

    @ActivityMethod
    PaymentIsoStatus transferToCorrespondentBank(CrossBoarderPaymentDetails input);

    @ActivityMethod
    PaymentIsoStatus recallFunds(CrossBoarderPaymentDetails input); // Recall funds

    @ActivityMethod
    PaymentIsoStatus creditBeneficiary(CrossBoarderPaymentDetails input);

    @ActivityMethod
    PaymentIsoStatus refundBeneficiary(CrossBoarderPaymentDetails input); // Refund beneficiary
}

package com.payments.frontdoor.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import model.PaymentDetails;
import model.PaymentInstruction;

@ActivityInterface
public interface AccountActivity {

    @ActivityMethod
    PaymentInstruction initiatePayment(PaymentDetails input);

    @ActivityMethod
    boolean managePaymentOrder(PaymentInstruction instruction);

    @ActivityMethod
    boolean authorizePayment(PaymentInstruction instruction);

    @ActivityMethod
    String executePayment(PaymentInstruction instruction);

    @ActivityMethod
    String clearAndSettlePayment(PaymentInstruction instruction);

    @ActivityMethod
    String sendNotification(PaymentInstruction instruction);

    @ActivityMethod
    String reconcilePayment(PaymentInstruction instruction);

    @ActivityMethod
    String postPayment(PaymentInstruction instruction);

    @ActivityMethod
    String generateReports(PaymentInstruction instruction);

    @ActivityMethod
    String archivePayment(PaymentInstruction instruction);

    @ActivityMethod
    String refundPayment(PaymentInstruction instruction);
}

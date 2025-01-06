package com.payments.frontdoor.activities;

import com.payments.frontdoor.swagger.model.PaymentResponse.StatusEnum;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import model.PaymentDetails;
import model.PaymentInstruction;

@ActivityInterface
public interface PaymentActivity {

    @ActivityMethod
    PaymentInstruction initiatePayment(PaymentDetails input);

    @ActivityMethod
    boolean managePaymentOrder(PaymentInstruction instruction);

    @ActivityMethod
    boolean authorizePayment(PaymentInstruction instruction);

    @ActivityMethod
    StatusEnum executePayment(PaymentInstruction instruction);

    @ActivityMethod
    StatusEnum clearAndSettlePayment(PaymentInstruction instruction);

    @ActivityMethod
    StatusEnum sendNotification(PaymentInstruction instruction);

    @ActivityMethod
    StatusEnum reconcilePayment(PaymentInstruction instruction);

    @ActivityMethod
    StatusEnum postPayment(PaymentInstruction instruction);

    @ActivityMethod
    StatusEnum generateReports(PaymentInstruction instruction);

    @ActivityMethod
    StatusEnum archivePayment(PaymentInstruction instruction);

    @ActivityMethod
    StatusEnum refundPayment(PaymentInstruction instruction);
}
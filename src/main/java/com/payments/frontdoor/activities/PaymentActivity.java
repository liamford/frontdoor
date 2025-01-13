package com.payments.frontdoor.activities;

import com.payments.frontdoor.model.PaymentDetails;
import com.payments.frontdoor.model.PaymentInstruction;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface PaymentActivity {

    @ActivityMethod
    PaymentInstruction initiatePayment(PaymentDetails input);

    @ActivityMethod
    boolean managePaymentOrder(PaymentInstruction instruction);

    @ActivityMethod
    boolean authorizePayment(PaymentInstruction instruction);

    @ActivityMethod
    PaymentStepStatus executePayment(PaymentInstruction instruction);

    @ActivityMethod
    PaymentStepStatus clearAndSettlePayment(PaymentInstruction instruction);

    @ActivityMethod
    PaymentStepStatus sendNotification(PaymentInstruction instruction);

    @ActivityMethod
    PaymentStepStatus reconcilePayment(PaymentInstruction instruction);

    @ActivityMethod
    PaymentStepStatus postPayment(PaymentInstruction instruction);

    @ActivityMethod
    PaymentStepStatus generateReports(PaymentInstruction instruction);

    @ActivityMethod
    PaymentStepStatus archivePayment(PaymentInstruction instruction);

    @ActivityMethod
    PaymentStepStatus refundPayment(PaymentInstruction instruction);
}
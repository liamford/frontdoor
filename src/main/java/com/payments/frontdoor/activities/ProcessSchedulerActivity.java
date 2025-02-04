package com.payments.frontdoor.activities;


import com.payments.frontdoor.model.PaymentDetails;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ProcessSchedulerActivity {

    @ActivityMethod
    String submitPayments(PaymentDetails input);

    @ActivityMethod
    String healthCheck();

    @ActivityMethod
    String notifyBatchJobStatus(String paymentType);

}

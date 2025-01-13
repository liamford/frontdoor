package com.payments.frontdoor.workflows;


import com.payments.frontdoor.activities.PaymentStepStatus;
import com.payments.frontdoor.swagger.model.PaymentResponse;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import com.payments.frontdoor.model.PaymentDetails;

import java.util.Set;

@WorkflowInterface
public interface PaymentWorkflow {

    @WorkflowMethod
    PaymentResponse processPayment(PaymentDetails input);

    @SignalMethod
    void waitForStep(PaymentStepStatus paymentStepStatus);

    @QueryMethod
    Set<PaymentStepStatus> getCompletedSteps();

}

package com.payments.frontdoor.workflows;


import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import model.PaymentDetails;

@WorkflowInterface
public interface SendPaymentWorkflow {

    @WorkflowMethod
    void processPayment(PaymentDetails input);

}

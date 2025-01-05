package com.payments.frontdoor.workflows;


import com.payments.frontdoor.swagger.model.PaymentResponse;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import model.PaymentDetails;

@WorkflowInterface
public interface SendPaymentWorkflow {

    @WorkflowMethod
    PaymentResponse processPayment(PaymentDetails input);

}

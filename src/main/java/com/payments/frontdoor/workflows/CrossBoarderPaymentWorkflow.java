package com.payments.frontdoor.workflows;


import com.payments.frontdoor.model.CrossBoarderPaymentDetails;
import com.payments.frontdoor.swagger.model.PaymentResponse;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CrossBoarderPaymentWorkflow {

    @WorkflowMethod
    PaymentResponse processPayment(CrossBoarderPaymentDetails input);

}

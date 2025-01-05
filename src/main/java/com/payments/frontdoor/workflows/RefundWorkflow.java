package com.payments.frontdoor.workflows;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import model.PaymentInstruction;

@WorkflowInterface
public interface RefundWorkflow {

    @WorkflowMethod
    void processRefund(PaymentInstruction instruction);

}

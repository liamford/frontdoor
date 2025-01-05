package com.payments.frontdoor.workflows;

import com.payments.frontdoor.swagger.model.PaymentResponse;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import model.PaymentInstruction;

@WorkflowInterface
public interface RefundWorkflow {

    @WorkflowMethod
    PaymentResponse processRefund(PaymentInstruction instruction);

}

package com.payments.frontdoor.workflows;

import com.payments.frontdoor.model.PaymentInstruction;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ReportWorkflow {

    @WorkflowMethod
    String processReporting(PaymentInstruction instruction);

}

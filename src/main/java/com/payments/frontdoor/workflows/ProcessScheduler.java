package com.payments.frontdoor.workflows;


import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ProcessScheduler {

    @WorkflowMethod
    String batchPaymentProcessor(String paymentType);
}

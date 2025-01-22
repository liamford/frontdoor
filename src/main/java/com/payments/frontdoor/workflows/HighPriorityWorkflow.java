package com.payments.frontdoor.workflows;


import com.payments.frontdoor.activities.PaymentStepStatus;
import com.payments.frontdoor.model.PaymentDetails;
import com.payments.frontdoor.swagger.model.PaymentResponse;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.Set;

@WorkflowInterface
public interface HighPriorityWorkflow {

    @WorkflowMethod
    PaymentResponse processPayment(PaymentDetails input);


    @QueryMethod
    Set<PaymentStepStatus> getCompletedSteps();

}

package com.payments.frontdoor.workflows;

import com.payments.frontdoor.util.PaymentUtil;
import com.payments.frontdoor.activities.PaymentActivity;
import com.payments.frontdoor.swagger.model.PaymentResponse;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;
import com.payments.frontdoor.model.PaymentInstruction;

import java.time.Duration;

import static com.payments.frontdoor.util.PaymentUtil.startReportWorkflow;

@WorkflowImpl(workers = "send-payment-worker")
public class RefundWorkflowImpl implements RefundWorkflow {

    // RetryOptions specify how to automatically handle retries when Activities fail
    private final RetryOptions retryoptions = RetryOptions.newBuilder()
            .setInitialInterval(Duration.ofSeconds(1)) // Wait 1 second before first retry
            .setMaximumInterval(Duration.ofSeconds(20)) // Do not exceed 20 seconds between retries
            .setBackoffCoefficient(2) // Wait 1 second, then 2, then 4, etc
            .setMaximumAttempts(5000) // Fail after 5000 attempts
            .build();

    // ActivityOptions specify the limits on how long an Activity can execute before
    // being interrupted by the Orchestration service
    private final ActivityOptions defaultActivityOptions = ActivityOptions.newBuilder()
            .setRetryOptions(retryoptions) // Apply the RetryOptions defined above
            .setStartToCloseTimeout(Duration.ofSeconds(2)) // Max execution time for single Activity
            .setScheduleToCloseTimeout(Duration.ofSeconds(5000)) // Entire duration from scheduling to completion including queue time
            .build();


    // ActivityStubs enable calls to methods as if the Activity object is local but actually perform an RPC invocation
    private final PaymentActivity activities = Workflow.newActivityStub(PaymentActivity.class, defaultActivityOptions);

    @Override
    public PaymentResponse processRefund(PaymentInstruction instruction) {
        activities.refundPayment(instruction);
        activities.reconcilePayment(instruction);
        activities.sendNotification(instruction);
        startReportWorkflow(instruction);
        return PaymentUtil.createPaymentResponse(instruction.getPaymentId(), PaymentResponse.StatusEnum.ACSC);
    }
}

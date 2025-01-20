package com.payments.frontdoor.workflows;


import com.payments.frontdoor.activities.PaymentStepStatus;
import com.payments.frontdoor.exception.*;
import com.payments.frontdoor.util.PaymentUtil;
import com.payments.frontdoor.activities.PaymentActivity;
import com.payments.frontdoor.swagger.model.PaymentResponse;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Async;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.payments.frontdoor.model.PaymentDetails;
import com.payments.frontdoor.model.PaymentInstruction;

import java.time.Duration;
import java.util.*;

@WorkflowImpl(workers = "send-payment-worker")
@Slf4j
@NoArgsConstructor
public class PaymentWorkflowImpl implements PaymentWorkflow {
    private static final String INITIATE = "initiatePayment";
    private Set<PaymentStepStatus> steps;


    // RetryOptions specify how to automatically handle retries when Activities fail
    private final RetryOptions retryoptions = RetryOptions.newBuilder()
            .setInitialInterval(Duration.ofSeconds(1)) // Wait 1 second before first retry
            .setMaximumInterval(Duration.ofSeconds(20)) // Do not exceed 20 seconds between retries
            .setBackoffCoefficient(2) // Wait 1 second, then 2, then 4, etc
            .setDoNotRetry(IllegalArgumentException.class.getName(),
                    NullPointerException.class.getName(),
                    PaymentAuthorizationFailedException.class.getName(),
                    PaymentBadRequestException.class.getName(),
                    PaymentUnauthorizedException.class.getName(),
                    PaymentForbiddenException.class.getName(),
                    PaymentConflictException.class.getName(),
                    PaymentClientException.class.getName()
            ) // Do not retry for these exceptions
            .setMaximumAttempts(5) // Fail after 5000 attempts
            .build();

    // ActivityOptions specify the limits on how long an Activity can execute before
    // being interrupted by the Orchestration service
    private final ActivityOptions defaultActivityOptions = ActivityOptions.newBuilder()
            .setRetryOptions(retryoptions) // Apply the RetryOptions defined above
            .setStartToCloseTimeout(Duration.ofSeconds(2)) // Max execution time for single Activity
            .setScheduleToCloseTimeout(Duration.ofSeconds(5000)) // Entire duration from scheduling to completion including queue time
            .build();

    private final Map<String, ActivityOptions> perActivityMethodOptions = new HashMap<String, ActivityOptions>() {{
        // A heartbeat time-out is a proof-of life indicator that an activity is still working.
        // The 5 second duration used here waits for up to 5 seconds to hear a heartbeat.
        // If one is not heard, the Activity fails.
        // The `initiatePayment` method is hard-coded to succeed, so this never happens.
        // Use heartbeats for long-lived event-driven applications.
        put(INITIATE, ActivityOptions.newBuilder().setHeartbeatTimeout(Duration.ofSeconds(5)).build());
    }};

    // ActivityStubs enable calls to methods as if the Activity object is local but actually perform an RPC invocation
    private final PaymentActivity activities = Workflow.newActivityStub(PaymentActivity.class, defaultActivityOptions, perActivityMethodOptions);


    @Override
    public PaymentResponse processPayment(PaymentDetails paymentDetails) {
        steps = new LinkedHashSet<>();
        PaymentInstruction instruction = activities.initiatePayment(paymentDetails);
        steps.add(PaymentStepStatus.INITIATED);
        // Step 2 & 3: Run Payment Order Management and Payment Authorization in Parallel

        try {
            Promise<Boolean> isOrderValidPromise = Async.function(activities::managePaymentOrder, instruction);
            Promise<Boolean> isAuthorizedPromise = Async.function(activities::authorizePayment, instruction);

            Promise.allOf(isAuthorizedPromise, isOrderValidPromise).get();
            Workflow.getLogger(PaymentWorkflowImpl.class).info("Payment successfully validated and authorized.");

        } catch (PaymentAuthorizationFailedException e) {
            log.error("Payment validation or authorization failed: ", e);
            Workflow.getLogger(PaymentWorkflowImpl.class).error("Payment validation or authorization failed: ", e);
            throw Workflow.wrap(e);
        }

        // Step 4: Execute Payment
        activities.executePayment(instruction);
        Workflow.await(() -> this.steps.contains(PaymentStepStatus.EXECUTED));

        // Steps 5, 6, & 7: Run Clearing, Notification, and Reconciliation in Parallel
        Promise<PaymentStepStatus> clearAndSettlePromise = Async.function(activities::clearAndSettlePayment, instruction);
        Promise<PaymentStepStatus> sendNotificationPromise = Async.function(activities::sendNotification, instruction);
        Promise<PaymentStepStatus> reconcilePromise = Async.function(activities::reconcilePayment, instruction);

        Promise.allOf(clearAndSettlePromise, sendNotificationPromise, reconcilePromise).get();
        steps.add(clearAndSettlePromise.get());
        steps.add(sendNotificationPromise.get());
        steps.add(reconcilePromise.get());

        // Step 8: Post Payment
        try {
            steps.add(activities.postPayment(instruction));

        } catch (Exception e) {
            Workflow.getLogger(PaymentWorkflowImpl.class).error("Post-payment failed: ", e);
            // Trigger refund sub-workflow
            ChildWorkflowOptions childWorkflowOptions = ChildWorkflowOptions.newBuilder()
                    .setWorkflowId(instruction.getPaymentReference() + "-refund")
                    .build();
            RefundWorkflow refundWorkflow = Workflow.newChildWorkflowStub(RefundWorkflow.class, childWorkflowOptions);

            refundWorkflow.processRefund(instruction);
            throw Workflow.wrap(e);
        }

        // Step 9: Generate Reports
        steps.add(activities.generateReports(instruction));

        // Step 10: Archive Payment
        steps.add(activities.archivePayment(instruction));

        Workflow.getLogger(PaymentWorkflowImpl.class).info("Payment processing workflow completed.");

        return PaymentUtil.createPaymentResponse(instruction.getPaymentId(), PaymentResponse.StatusEnum.ACSC);

    }

    @Override
    public void waitForStep(PaymentStepStatus paymentStepStatus) {
        steps.add(paymentStepStatus);
    }

    @Override
    public Set<PaymentStepStatus> getCompletedSteps() {
        return steps;
    }
}

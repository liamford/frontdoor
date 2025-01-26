package com.payments.frontdoor.workflows;

import com.payments.frontdoor.activities.CrossBoarderPaymentActivity;
import com.payments.frontdoor.exception.*;
import com.payments.frontdoor.model.CrossBoarderPaymentDetails;
import com.payments.frontdoor.swagger.model.PaymentResponse;
import com.payments.frontdoor.util.PaymentUtil;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@WorkflowImpl(workers = "cb-payment-worker")
@Slf4j
@NoArgsConstructor

public class CrossBoarderPaymentWorkflowImpl implements CrossBoarderPaymentWorkflow {

    private static final String INITIATE = "initiatePayment";

    // RetryOptions specify how to automatically handle retries when Activities fail
    private final RetryOptions retryoptions = RetryOptions.newBuilder()
            .setInitialInterval(Duration.ofSeconds(1)) // Wait 1 second before first retry
            .setMaximumInterval(Duration.ofSeconds(20)) // Do not exceed 20 seconds between retries
            .setBackoffCoefficient(2) // Wait 1 second, then 2, then 4, etc
            .setDoNotRetry(IllegalArgumentException.class.getName(),
                    NullPointerException.class.getName(),
                    PaymentProcessingException.class.getName(),
                    PaymentAuthorizationFailedException.class.getName(),
                    PaymentOrderFailedException.class.getName(),
                    PaymentBadRequestException.class.getName(),
                    PaymentUnauthorizedException.class.getName(),
                    PaymentForbiddenException.class.getName(),
                    PaymentConflictException.class.getName(),
                    PaymentClientException.class.getName()
            ) // Do not retry for these exceptions
            .setMaximumAttempts(5) // Fail after 5 attempts
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
    private final CrossBoarderPaymentActivity activities = Workflow.newActivityStub(CrossBoarderPaymentActivity.class, defaultActivityOptions, perActivityMethodOptions);

    @Override
    public PaymentResponse processPayment(CrossBoarderPaymentDetails input) {

        Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());

        try {
            // Step 1: Debit Customer Account
            saga.addCompensation(() -> activities.debitCompensation(input));
            activities.debitAccount(input);

            // Step 2: Reserve Foreign Currency
            saga.addCompensation(() -> activities.releaseCurrency(input));
            activities.reserveCurrency(input);

            // Step 3: Perform Sanctions Check
            activities.performSanctionsCheck(input); // No compensation needed

            // Step 4: Transfer Funds to Correspondent Bank
            saga.addCompensation(() -> activities.recallFunds(input));
            activities.transferToCorrespondentBank(input);

            // Step 5: Credit Beneficiary Account
            saga.addCompensation(() -> activities.refundBeneficiary(input));
            activities.creditBeneficiary(input);
        } catch (Exception e) {
            // Automatically triggers compensations in reverse order
            saga.compensate();
            throw Workflow.wrap(e);
        }

        Workflow.getLogger(CrossBoarderPaymentWorkflowImpl.class).info("Payment processing workflow completed.");
        return PaymentUtil.createPaymentResponse(input.getPaymentId(), PaymentResponse.StatusEnum.ACSC);

    }
}

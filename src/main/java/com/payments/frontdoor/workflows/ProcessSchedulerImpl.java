package com.payments.frontdoor.workflows;


import com.payments.frontdoor.activities.ProcessSchedulerActivity;
import com.payments.frontdoor.model.PaymentDetails;
import com.payments.frontdoor.model.PaymentPriority;
import com.payments.frontdoor.swagger.model.Account;
import com.payments.frontdoor.swagger.model.PaymentResponse;
import com.payments.frontdoor.util.PaymentUtil;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@WorkflowImpl(workers = "normal-payment-worker")
@Slf4j
@NoArgsConstructor
public class ProcessSchedulerImpl implements ProcessScheduler {

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
    private final ProcessSchedulerActivity activities = Workflow.newActivityStub(ProcessSchedulerActivity.class, defaultActivityOptions);


    @Override
    public String batchPaymentProcessor(String paymentType) {

        int numberOfPayments = ThreadLocalRandom.current().nextInt(5, 11);
        log.info("Number of payments to process for Payment Type : {} = {}", paymentType, numberOfPayments);
        activities.healthCheck();
        IntStream.range(0, numberOfPayments).mapToObj(payment -> createPaymentDetails()).forEach(activities::submitPayments);
        activities.notifyBatchJobStatus(paymentType);

        return "Batch Payment Processing Completed";
    }

    private PaymentDetails createPaymentDetails() {
        String uetr = PaymentUtil.generateUetr();

        Account debtor = new Account();
        debtor.setAccountNumber(PaymentUtil.generateUetr());
        debtor.setAccountName("John Doe");
        Account creditor = new Account();
        creditor.setAccountNumber(PaymentUtil.generateUetr());
        creditor.setAccountName("Jane Doe");

        return PaymentDetails.builder()
            .paymentId(uetr)
            .amount(new BigDecimal("100.00"))
            .currency("AUD")
            .paymentStatus(PaymentResponse.StatusEnum.ACTC.toString())
            .paymentReference("REF123")
            .debtor(debtor)
            .creditor(creditor)
            .priority(PaymentPriority.NORMAL)
            .paymentDate(LocalDate.now())
            .headers(Map.of(
                "X-Correlation-ID", "123456",
                "x-idempotency-key", uetr,
                "Content-Type", "application/json",
                "x-request-status", "201"
            ))
            .build();
    }
}

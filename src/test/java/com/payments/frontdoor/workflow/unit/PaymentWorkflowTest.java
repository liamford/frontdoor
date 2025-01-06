package com.payments.frontdoor.workflow.unit;

import com.payments.frontdoor.activities.PaymentActivityImpl;
import com.payments.frontdoor.swagger.model.Account;
import com.payments.frontdoor.swagger.model.PaymentResponse;
import com.payments.frontdoor.workflows.PaymentWorkflow;
import com.payments.frontdoor.workflows.PaymentWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.testing.WorkflowInitialTime;
import io.temporal.worker.Worker;
import model.PaymentDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class PaymentWorkflowTest {

    @RegisterExtension
    public static final TestWorkflowExtension testWorkflow =
            TestWorkflowExtension.newBuilder()
                    .setWorkflowTypes(PaymentWorkflowImpl.class)
                    .setActivityImplementations(new PaymentActivityImpl())
                    .setInitialTime(Instant.parse("2021-10-10T10:01:00Z"))
                    .build();

    @Test
    @WorkflowInitialTime("2020-01-01T01:00:00Z")
    public void testProcessPayment(
            TestWorkflowEnvironment testEnv,
            WorkflowClient workflowClient,
            WorkflowOptions workflowOptions,
            Worker worker,
            PaymentWorkflow workflow) {

        Account debtor = new Account();
        debtor.setAccountNumber("123456789");
        debtor.setAccountName("John Doe");
        Account creditor = new Account();
        creditor.setAccountNumber("987654321");
        creditor.setAccountName("Jane Doe");

        PaymentDetails paymentDetails = PaymentDetails.builder()
                .paymentId("12345")
                .amount(new BigDecimal("100.00"))
                .currency("AUD")
                .paymentReference("REF123")
                .debtor(debtor)
                .creditor(creditor)
                .paymentDate(LocalDate.now())
                .build();

        assertAll(
                () -> assertTrue(testEnv.isStarted()),
                () -> assertNotNull(workflowClient),
                () -> assertNotNull(workflowOptions.getTaskQueue()),
                () -> assertNotNull(worker),
                () -> assertEquals(
                        Instant.parse("2020-01-01T01:00:00Z"),
                        Instant.ofEpochMilli(testEnv.currentTimeMillis()).truncatedTo(ChronoUnit.HOURS)),
                () -> {
                    PaymentResponse response = workflow.processPayment(paymentDetails);
                    assertNotNull(response);
                    assertEquals(PaymentResponse.StatusEnum.ACSC, response.getStatus());
                });
    }
}

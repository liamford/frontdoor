package com.payments.frontdoor.workflow.unit;

import com.payments.frontdoor.activities.PaymentActivityImpl;
import com.payments.frontdoor.activities.PaymentStepStatus;
import com.payments.frontdoor.model.PaymentAuthorizationResponse;
import com.payments.frontdoor.model.PaymentDetails;
import com.payments.frontdoor.model.PaymentInstruction;
import com.payments.frontdoor.model.PaymentOrderResponse;
import com.payments.frontdoor.service.PaymentApiConnector;
import com.payments.frontdoor.service.PaymentDispatcherService;
import com.payments.frontdoor.swagger.model.Account;
import com.payments.frontdoor.swagger.model.PaymentResponse;
import com.payments.frontdoor.workflows.PaymentWorkflow;
import com.payments.frontdoor.workflows.PaymentWorkflowImpl;
import com.payments.frontdoor.workflows.RefundWorkflowImpl;
import com.payments.frontdoor.workflows.ReportWorkflowImpl;
import io.temporal.client.ActivityCompletionClient;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.testing.WorkflowInitialTime;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
@RunWith(MockitoJUnitRunner.class)
class PaymentWorkflowTest {

    private static final Logger log = LoggerFactory.getLogger(PaymentWorkflowTest.class);
    private final PaymentDispatcherService paymentDispatcherService = mock(PaymentDispatcherService.class);
    private final PaymentApiConnector paymentApiConnector = mock(PaymentApiConnector.class);

    @RegisterExtension
    public final TestWorkflowExtension testWorkflow =
            TestWorkflowExtension.newBuilder()
                    .setWorkflowTypes(PaymentWorkflowImpl.class, RefundWorkflowImpl.class, ReportWorkflowImpl.class)
                    .setActivityImplementations(new PaymentActivityImpl(paymentApiConnector, paymentDispatcherService))
                    .setInitialTime(Instant.parse("2021-10-10T10:01:00Z"))
                    .build();

    @Test
    @WorkflowInitialTime("2020-01-01T01:00:00Z")
    void testPositiveProcessPayment(
            TestWorkflowEnvironment testEnv,
            WorkflowClient workflowClient,
            WorkflowOptions workflowOptions,
            Worker worker,
            PaymentWorkflow workflow) {

        when(paymentApiConnector.callOrderPayment(any(PaymentInstruction.class)))
                .thenReturn(PaymentOrderResponse.builder().status("completed").build());
        when(paymentApiConnector.callAuthorizePayment(any(PaymentInstruction.class)))
                .thenReturn(PaymentAuthorizationResponse.builder().status("success").build());

        doAnswer(invocation -> {
            byte[] receivedToken = invocation.getArgument(2);
            ActivityCompletionClient completionClient = workflowClient.newActivityCompletionClient();
            completionClient.complete(receivedToken, PaymentStepStatus.POSTED);
            return null;
        }).when(paymentDispatcherService).dispatchPayment(any(), eq(PaymentStepStatus.POSTED), any());

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
                .headers(Map.of(
                        "CORRELATION_ID", "123e4567-e89b-12d3-a456-426614174000",
                        "x-idempotency-key", "TRANS-12345",
                        "Content-Type", "application/json",
                        "x-request-status", "201"
                ))
                .build();

        WorkflowClient.start(workflow::processPayment, paymentDetails);
        workflow.waitForStep(PaymentStepStatus.EXECUTED);
        PaymentResponse response = workflow.processPayment(paymentDetails);

        assertAll(
                () -> assertTrue(testEnv.isStarted()),
                () -> assertNotNull(workflowClient),
                () -> assertNotNull(workflowOptions.getTaskQueue()),
                () -> assertNotNull(worker),
                () -> assertNotNull(response),
                () -> assertEquals(PaymentResponse.StatusEnum.ACSC, response.getStatus()),
                () -> assertNotNull(workflow.getCompletedSteps()),
                () -> assertEquals(
                        Instant.parse("2020-01-01T01:00:00Z"),
                        Instant.ofEpochMilli(testEnv.currentTimeMillis()).truncatedTo(ChronoUnit.HOURS))
        );
    }
}
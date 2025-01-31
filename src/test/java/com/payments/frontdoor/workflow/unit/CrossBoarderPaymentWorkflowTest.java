package com.payments.frontdoor.workflow.unit;

import com.payments.frontdoor.activities.CrossBoarderPaymentActivityImpl;
import com.payments.frontdoor.model.CrossBoarderPaymentDetails;
import com.payments.frontdoor.swagger.model.*;
import com.payments.frontdoor.workflows.CrossBoarderPaymentWorkflow;
import com.payments.frontdoor.workflows.CrossBoarderPaymentWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowException;
import io.temporal.client.WorkflowOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.testing.WorkflowInitialTime;
import io.temporal.worker.Worker;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
@RunWith(MockitoJUnitRunner.class)
class CrossBoarderPaymentWorkflowTest {


    @Rule
    public TestWorkflowRule testWorkflowRule =
            TestWorkflowRule.newBuilder()
                    .setWorkflowTypes(CrossBoarderPaymentWorkflowImpl.class)
                    .setDoNotStart(true)
                    .build();


    @RegisterExtension
    public final TestWorkflowExtension testWorkflow =
            TestWorkflowExtension.newBuilder()
                    .setWorkflowTypes(CrossBoarderPaymentWorkflowImpl.class)
                    .setActivityImplementations(new CrossBoarderPaymentActivityImpl())
                    .setInitialTime(Instant.parse("2021-10-10T10:01:00Z"))
                    .build();

    @Test
    @WorkflowInitialTime("2020-01-01T01:00:00Z")
    void testPositiveCBPayment(
            TestWorkflowEnvironment testEnv,
            WorkflowClient workflowClient,
            WorkflowOptions workflowOptions,
            Worker worker,
            CrossBoarderPaymentWorkflow workflow) {


        CrossBoarderPaymentDetails paymentDetails = CrossBoarderPaymentDetails.builder()
                .paymentId("123456")
                .paymentReference("Ref123456")
                .paymentStatus("Pending")
                .customer(createCustomer())
                .beneficiary(createBeneficiary())
                .transactionDetails(createTransactionDetails())
                .fees(createFees())
                .headers(Map.of(
                        "x-correlation-id", "123e4567-e89b-12d3-a456-426614174000",
                        "x-idempotency-key", "TRANS-12345",
                        "Content-Type", "application/json",
                        "x-request-status", "201"))
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

                });
    }


    @Test
    void testMissingCorrelationIdFails(
            TestWorkflowEnvironment testEnv, CrossBoarderPaymentWorkflow workflow) {

        CrossBoarderPaymentDetails paymentDetails = CrossBoarderPaymentDetails.builder()
                .paymentId("123456")
                .paymentReference("Ref123456")
                .paymentStatus("Pending")
                .customer(createCustomer())
                .beneficiary(createBeneficiary())
                .transactionDetails(createTransactionDetails())
                .fees(createFees())
                .headers(Map.of(
                        "x-idempotency-key", "TRANS-12345",
                        "Content-Type", "application/json",
                        "x-request-status", "201"))
                .build();

        testEnv.start();

        WorkflowException exception =
                assertThrows(WorkflowException.class, () -> workflow.processPayment(paymentDetails));
        assertEquals(
                "Payment processing failed",
                ((ApplicationFailure) exception.getCause().getCause()).getOriginalMessage());
    }


    @Test
    void testDebitFails(
            TestWorkflowEnvironment testEnv, CrossBoarderPaymentWorkflow workflow) {

        CrossBoarderPaymentDetails paymentDetails = CrossBoarderPaymentDetails.builder()
                .paymentId("123456")
                .paymentReference("Ref123456")
                .paymentStatus("Pending")
                .customer(createCustomer())
                .beneficiary(createBeneficiary())
                .transactionDetails(createTransactionDetails())
                .fees(createFees())
                .headers(Map.of(
                        "x-correlation-id", "123e4567-e89b-12d3-a456-debit-error",
                        "x-idempotency-key", "TRANS-12345",
                        "Content-Type", "application/json",
                        "x-request-status", "201"))
                .build();

        testEnv.start();

        WorkflowException exception =
                assertThrows(WorkflowException.class, () -> workflow.processPayment(paymentDetails));
        assertEquals(
                "Payment processing failed",
                ((ApplicationFailure) exception.getCause().getCause()).getOriginalMessage());
    }





    private CrossBorderPaymentRequestCustomer createCustomer() {
        CrossBorderPaymentRequestCustomer customer = new CrossBorderPaymentRequestCustomer();
        customer.setAccountNumber("123456789");
        customer.setCustomerId("123");
        customer.setName("John Doe");
        return customer;
    }

    private CrossBorderPaymentRequestBeneficiary createBeneficiary() {
        CrossBorderPaymentRequestBeneficiary beneficiary = new CrossBorderPaymentRequestBeneficiary();
        beneficiary.setAccountNumber("987654321");
        beneficiary.setName("Jane Smith");
        beneficiary.setBankCode("Bank of America");
        return beneficiary;
    }

    private CrossBorderPaymentRequestTransactionDetails createTransactionDetails() {
        CrossBorderPaymentRequestTransactionDetails transactionDetails = new CrossBorderPaymentRequestTransactionDetails();
        transactionDetails.setExchangeRate(1.0);
        transactionDetails.setForeignCurrencyAmount(100D);
        transactionDetails.description("Payment for goods");
        return transactionDetails;
    }

    private CrossBorderPaymentRequestFees createFees() {
        CrossBorderPaymentRequestFees fees = new CrossBorderPaymentRequestFees();
        fees.setTransferFee(10D);
        fees.setTotalDebit(110D);
        return fees;
    }


}

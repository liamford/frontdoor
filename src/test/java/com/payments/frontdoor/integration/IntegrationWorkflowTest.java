package com.payments.frontdoor.integration;

import com.payments.frontdoor.FrontdoorApplication;
import com.payments.frontdoor.model.ActivityResult;
import com.payments.frontdoor.model.PaymentStatus;
import com.payments.frontdoor.service.PaymentProcessService;
import com.payments.frontdoor.swagger.model.*;
import com.payments.frontdoor.util.PaymentUtil;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("integration")
public class IntegrationWorkflowTest {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationWorkflowTest.class);
    private static final String BASE_URL = "http://localhost:8080/api/payments/v1";
    private static final RestTemplate restTemplate = new RestTemplate();


    @Autowired
    private PaymentProcessService paymentProcessService;

    @BeforeAll
    public static void setup() {
        logger.info("Starting integration tests...");
        assertDoesNotThrow(() -> FrontdoorApplication.main(new String[]{}));
    }

    @AfterAll
    public static void cleanup() {
        logger.info("Integration tests completed.");
    }

    @Test
    @DisplayName("Health Check Test")
    void healthCheck() {
        ResponseEntity<String> response = restTemplate.getForEntity(BASE_URL + "/actuator", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Health check failed.");
        logger.info("Health Check Response: {}", response.getBody());
    }

    @Test
    @DisplayName("Asynchronous Payment Submission Test")
    void aSyncPaymentSubmitCheck() {
        Account debtor = new Account();
        debtor.setAccountName("Liam Ford");
        debtor.setAccountNumber("123456");
        Account creditor = new Account();
        creditor.setAccountName("John Doe");
        creditor.setAccountNumber("123457");
        PaymentResponse paymentResponse = submitPaymentRequest(PaymentStatus.ASYNC,  debtor, creditor);
        assertNotNull(paymentResponse.getPaymentId(), "Payment ID should not be null.");
        assertEquals(PaymentResponse.StatusEnum.ACTC, paymentResponse.getStatus(), "Expected status ACTC.");
        waitForPaymentCompletion(paymentResponse.getPaymentId());
        verifyReportStatus(paymentResponse.getPaymentId());
    }

    @Test
    @DisplayName("Synchronous Payment Submission Test")
    void syncPaymentSubmitCheck() {
        Account debtor = new Account();
        debtor.setAccountName("Liam Ford");
        debtor.setAccountNumber("123456");
        Account creditor = new Account();
        creditor.setAccountName("John Doe");
        creditor.setAccountNumber("123457");
        PaymentResponse paymentResponse = submitPaymentRequest(PaymentStatus.SYNC, debtor, creditor);
        assertNotNull(paymentResponse.getPaymentId(), "Payment ID should not be null.");
        assertEquals(PaymentResponse.StatusEnum.ACSC, paymentResponse.getStatus(), "Expected status ACSC.");
        waitForPaymentCompletion(paymentResponse.getPaymentId());
        verifyReportStatus(paymentResponse.getPaymentId());
    }

    @Test
    @DisplayName("Synchronous Payment Submission Rejection Test")
    void syncPaymentSubmitCheckReject() {
        Account debtor = new Account();
        debtor.setAccountName("Liam Ford");
        debtor.setAccountNumber("123456");
        PaymentResponse paymentResponse = submitPaymentRequest(PaymentStatus.SYNC, debtor, debtor);
        assertNotNull(paymentResponse.getPaymentId(), "Payment ID should not be null.");
        assertEquals(PaymentResponse.StatusEnum.RJCT, paymentResponse.getStatus(), "Expected status ACSC.");
        verifyRefundStatus(paymentResponse.getPaymentId());
        verifyReportStatus(paymentResponse.getPaymentId());
    }


    private void verifyRefundStatus(String paymentId) {
        String refundId = paymentId + "-refund";
        assertTrue(
            WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING.equals(paymentProcessService.getWorkflowStatus(refundId)) ||
                WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED.equals(paymentProcessService.getWorkflowStatus(refundId)),
            "Expected status RUNNING or COMPLETED."
        );
    }

    private void verifyReportStatus(String paymentId) {
        String reportId = paymentId + "-report";
        assertTrue(
            WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING.equals(paymentProcessService.getWorkflowStatus(reportId)) ||
                WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED.equals(paymentProcessService.getWorkflowStatus(reportId)),
            "Expected status RUNNING or COMPLETED."
        );
    }

    private PaymentResponse submitPaymentRequest(PaymentStatus status , Account debtor, Account creditor) {
        HttpHeaders headers = createHeaders(PaymentUtil.generateUetr(), "INV123456", status);
        HttpEntity<PaymentRequest> requestEntity = new HttpEntity<>(buildPaymentRequest(debtor, creditor), headers);

        ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(
            BASE_URL + "/submit-payment", requestEntity, PaymentResponse.class);

        assertNotNull(response.getBody(), "PaymentResponse should not be null.");
        return response.getBody();
    }

    private void waitForPaymentCompletion(String paymentId) {
        AtomicReference<PaymentStatusResponse> statusResponse = new AtomicReference<>(getPaymentStatus(paymentId));
       Awaitility.await()
              .atMost(Duration.ofSeconds(5))
              .pollInterval(Duration.ofMillis(500))
              .until(() -> {
                  statusResponse.set(getPaymentStatus(paymentId));
                  return PaymentStatusResponse.StatusEnum.ACSC.equals(statusResponse.get().getStatus());
              });

        validateActivities(statusResponse.get().getActivities());
    }

    private PaymentStatusResponse getPaymentStatus(String paymentId) {
        HttpHeaders headers = createHeaders("123456", null, PaymentStatus.ASYNC);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<PaymentStatusResponse> response = restTemplate.exchange(
            BASE_URL + "/payment-status/" + paymentId + "?includeActivities=true",
            HttpMethod.GET, entity, PaymentStatusResponse.class);

        assertNotNull(response.getBody(), "PaymentStatusResponse should not be null.");
        return response.getBody();
    }

    private void validateActivities(List<Activities> activities) {
        List<ActivityResult> expectedActivities = List.of(
            new ActivityResult("InitiatePayment", "successful", null),
            new ActivityResult("ManagePaymentOrder", "successful", null),
            new ActivityResult("AuthorizePayment", "successful", null),
            new ActivityResult("ExecutePayment", "successful", null),
            new ActivityResult("ClearAndSettlePayment", "successful", null),
            new ActivityResult("ReconcilePayment", "successful", null),
            new ActivityResult("SendNotification", "successful", null),
            new ActivityResult("PostPayment", "successful", null)
        );

        assertTrue(expectedActivities.size() == activities.size() &&
                expectedActivities.stream().allMatch(expected -> activities.stream()
                    .anyMatch(actual -> expected.getActivityName().equals(actual.getActivityName()) &&
                        expected.getStatus().equals(actual.getStatus()))),
            "Activities validation failed.");
    }

    private PaymentRequest buildPaymentRequest(Account debtor, Account creditor) {
        PaymentRequest request = new PaymentRequest();
        request.setDebtor(debtor);
        request.setCreditor(creditor);
        request.setAmount(BigDecimal.valueOf(100.5));
        request.setCurrency("USD");
        request.setPaymentReference("INV123456");
        request.setPriority(PaymentRequest.PriorityEnum.NORMAL);
        request.setPaymentDate(LocalDate.of(2022, 1, 12));

        return request;
    }

    private HttpHeaders createHeaders(String correlationId, String idempotencyKey, PaymentStatus status) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-correlation-id", correlationId);
        if (idempotencyKey != null) {
            headers.set("x-idempotency-key", idempotencyKey);
        }
        headers.set("x-request-status", status.getCode());
        return headers;
    }
}

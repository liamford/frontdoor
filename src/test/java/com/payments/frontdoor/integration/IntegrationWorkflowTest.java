package com.payments.frontdoor.integration;

import com.payments.frontdoor.FrontdoorApplication;
import com.payments.frontdoor.model.ActivityResult;
import com.payments.frontdoor.swagger.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    void healthCheck() {
        ResponseEntity<String> response = restTemplate.getForEntity(BASE_URL + "/actuator", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Health check failed.");
        logger.info("Health Check Response: {}", response.getBody());
    }

    @Test
    void aSyncPaymentSubmitCheck() {
        PaymentResponse paymentResponse = submitPaymentRequest();
        assertNotNull(paymentResponse.getPaymentId(), "Payment ID should not be null.");
        assertEquals(PaymentResponse.StatusEnum.ACTC, paymentResponse.getStatus(), "Expected status ACTC.");

        waitForPaymentCompletion(paymentResponse.getPaymentId());
    }

    private PaymentResponse submitPaymentRequest() {
        HttpHeaders headers = createHeaders("123456", "INV123456");
        HttpEntity<PaymentRequest> requestEntity = new HttpEntity<>(buildPaymentRequest(), headers);

        ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(
            BASE_URL + "/submit-payment", requestEntity, PaymentResponse.class);

        assertNotNull(response.getBody(), "PaymentResponse should not be null.");
        return response.getBody();
    }

    private void waitForPaymentCompletion(String paymentId) {
        AtomicReference<PaymentStatusResponse> statusResponse = new AtomicReference<>(getPaymentStatus(paymentId));

        assertTimeout(Duration.ofSeconds(5), () -> {
            while (!PaymentStatusResponse.StatusEnum.ACSC.equals(statusResponse.get().getStatus())) {
                statusResponse.set(getPaymentStatus(paymentId));
                Thread.sleep(500);
            }
        });

        validateActivities(statusResponse.get().getActivities());
    }

    private PaymentStatusResponse getPaymentStatus(String paymentId) {
        HttpHeaders headers = createHeaders("123456", null);
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

    private PaymentRequest buildPaymentRequest() {
        Account debtor = new Account();
        debtor.setAccountNumber("123456789");
        debtor.setAccountName("John Doe");
        Account creditor = new Account();
        creditor.setAccountNumber("987654321");
        creditor.setAccountName("Jane Smith");

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

    private HttpHeaders createHeaders(String correlationId, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-correlation-id", correlationId);
        if (idempotencyKey != null) {
            headers.set("x-idempotency-key", idempotencyKey);
        }
        headers.set("x-request-status", "201");
        return headers;
    }
}

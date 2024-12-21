package com.payments.frontdoor.controller.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.payments.frontdoor.exception.IdempotencyKeyMismatchException;
import com.payments.frontdoor.swagger.model.Account;
import com.payments.frontdoor.swagger.model.PaymentRequest;
import com.payments.frontdoor.swagger.model.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PaymentControllerIntegrationTest {


    @Autowired
    private MockMvc mockMvc;
    private PaymentRequest paymentRequest;

    @BeforeEach
    public void setUp() {
        paymentRequest = getSamplePaymentRequest();
    }

    @Test
    @DisplayName("Test for successful payment request")
    public void testSuccessfulPaymentRequest() throws Exception {
        mockMvc.perform(performPostRequest()
                        .header("x-correlation-id", "123456")
                        .header("x-idempotency-key", "INV123456"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(PaymentResponse.StatusEnum.ACTC.toString()));
    }

    @Test
    @DisplayName("Test for validating missing correlation ID in payment request")
    public void testPaymentCorrelationIdValidationError() throws Exception {
        mockMvc.perform(performPostRequest())
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MissingRequestHeaderException.class, result.getResolvedException()));
    }

    @Test
    @DisplayName("Test for validating missing idempotency Key in payment request")
    public void testPaymentIdempotencyKeyValidationError() throws Exception {
        mockMvc.perform(performPostRequest()
                        .header("x-correlation-id", "123456"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MissingRequestHeaderException.class, result.getResolvedException()));
    }

    @Test
    @DisplayName("Test for MisMatch idempotency Key in payment request")
    public void testPaymentIdempotencyKeyMismatchError() throws Exception {
        mockMvc.perform(performPostRequest()
                        .header("x-correlation-id", "123456")
                        .header("x-idempotency-key", "123456"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(IdempotencyKeyMismatchException.class, result.getResolvedException()));
    }




    private MockHttpServletRequestBuilder performPostRequest() throws JsonProcessingException {
        return post("/submit-payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getJsonRequest());
    }

    private String getJsonRequest() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper.writeValueAsString(paymentRequest);
    }




    private PaymentRequest getSamplePaymentRequest() {
        Account debtor = new Account();
        debtor.setAccountNumber("123456789");
        debtor.setAccountName("John Doe");
        Account creditor = new Account();
        creditor.setAccountNumber("987654321");
        creditor.setAccountName("Jane Doe");
        paymentRequest = new PaymentRequest();
        paymentRequest.setDebtor(debtor);
        paymentRequest.setCreditor(creditor);
        paymentRequest.setAmount(BigDecimal.valueOf(100.5));
        paymentRequest.setCurrency("USD");
        paymentRequest.setPaymentReference("INV123456");
        paymentRequest.setPaymentDate(LocalDate.parse("2022-01-12"));
        return paymentRequest;
    }

}

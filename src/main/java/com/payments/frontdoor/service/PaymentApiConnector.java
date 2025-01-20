package com.payments.frontdoor.service;


import com.payments.frontdoor.config.ApiConnectorCustomProperties;
import com.payments.frontdoor.model.PaymentAuthorizationRequest;
import com.payments.frontdoor.model.PaymentAuthorizationResponse;
import com.payments.frontdoor.model.PaymentInstruction;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@AllArgsConstructor
public class PaymentApiConnector {
    private final WebClient webClient;
    private final ApiConnectorCustomProperties apiConnectorCustomProperties;


    public PaymentAuthorizationResponse callAuthorizePayment(PaymentInstruction paymentInstruction) {

        PaymentAuthorizationRequest requestBody = convertToRequestBody(paymentInstruction);
        return webClient.post()
                .uri(apiConnectorCustomProperties.getAuthorize())
                .header("Content-Type", "application/json")
                .header("X-Correlation-ID", paymentInstruction.getHeaders().get("x-correlation-id"))
                .header("X-Idempotency-Key", paymentInstruction.getHeaders().get("x-idempotency-key"))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(PaymentAuthorizationResponse.class)
                .doOnNext(response -> {
                    // Log the response
                    log.info("Received Response: {}", response);
                })
                .doOnError(error -> {
                    // Log the error
                    log.error("Error during API call: {}", error.getMessage(), error);
                })
                .block(); // Use block() for synchronous calls
    }

    private PaymentAuthorizationRequest convertToRequestBody(PaymentInstruction paymentInstruction) {
        return PaymentAuthorizationRequest.builder()
                .method("authorize-payment")
                .paymentId(paymentInstruction.getPaymentId())
                .debtor(paymentInstruction.getDebtor())
                .creditor(paymentInstruction.getCreditor())
                .amount(paymentInstruction.getAmount().doubleValue())  // Converting BigDecimal to double
                .currency(paymentInstruction.getCurrency())
                .paymentReference(paymentInstruction.getPaymentReference())
                .paymentDate(paymentInstruction.getPaymentDate())
                .build();
    }

}

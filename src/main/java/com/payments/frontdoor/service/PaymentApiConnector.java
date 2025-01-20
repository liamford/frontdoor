package com.payments.frontdoor.service;


import com.payments.frontdoor.config.ApiConnectorCustomProperties;
import com.payments.frontdoor.exception.*;
import com.payments.frontdoor.model.PaymentAuthorizationRequest;
import com.payments.frontdoor.model.PaymentAuthorizationResponse;
import com.payments.frontdoor.model.PaymentInstruction;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

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
                .headers(httpHeaders -> paymentInstruction.getHeaders().forEach(httpHeaders::add))
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        handleClientError(response, paymentInstruction.getHeaders().get("x-correlation-id")))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        handleServerError(response, paymentInstruction.getHeaders().get("x-correlation-id")))
                .bodyToMono(PaymentAuthorizationResponse.class)
                .doOnNext(response -> log.info("Received Response: {}", response))
                .doOnError(error -> log.error("Error during API call: {}", error.getMessage(), error))
                .block(Duration.ofSeconds(30));
    }

    private Mono<? extends Throwable> handleServerError(ClientResponse response, String correlationId) {
        return response.bodyToMono(String.class)
                .flatMap(errorBody -> {
                    String errorMessage = String.format("Server error occurred. Status: %s, CorrelationId: %s, Body: %s",
                            response.statusCode(), correlationId, errorBody);
                    log.error(errorMessage);

                    return switch (response.statusCode().value()) {
                        case 502 -> Mono.error(new PaymentGatewayException(errorMessage));
                        case 503 -> Mono.error(new PaymentServiceUnavailableException(errorMessage));
                        default -> Mono.error(new PaymentServerException(errorMessage));
                    };
                });
    }

    private Mono<? extends Throwable> handleClientError(ClientResponse response, String correlationId) {
        return response.bodyToMono(String.class)
                .flatMap(errorBody -> {
                    String errorMessage = String.format("Client error occurred. Status: %s, CorrelationId: %s, Body: %s",
                            response.statusCode(), correlationId, errorBody);
                    log.error(errorMessage);

                    return switch (response.statusCode().value()) {
                        case 400 -> Mono.error(new PaymentBadRequestException(errorMessage));
                        case 401 -> Mono.error(new PaymentUnauthorizedException(errorMessage));
                        case 403 -> Mono.error(new PaymentForbiddenException(errorMessage));
                        case 404 -> Mono.error(new PaymentNotFoundException(errorMessage));
                        case 409 -> Mono.error(new PaymentConflictException(errorMessage));
                        default -> Mono.error(new PaymentClientException(errorMessage));
                    };
                });
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

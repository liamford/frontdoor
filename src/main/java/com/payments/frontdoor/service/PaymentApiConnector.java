package com.payments.frontdoor.service;

import com.payments.frontdoor.config.ApiConnectorCustomProperties;
import com.payments.frontdoor.exception.*;
import com.payments.frontdoor.model.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class PaymentApiConnector {
    private final WebClient webClient;
    private final ApiConnectorCustomProperties properties;

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final String CORRELATION_ID = "x-correlation-id";
    private static final String NORMAL_PRIORITY = "Normal";
    private static final String AUTHORIZE_PAYMENT_METHOD = "authorize-payment";

    /**
     * Calls the authorize payment endpoint
     * @param instruction Payment instruction containing the payment details
     * @return PaymentAuthorizationResponse The authorization response
     */
    public PaymentAuthorizationResponse callAuthorizePayment(PaymentInstruction instruction) {
        return executeRequest(
                properties.getAuthorize(),
                convertToAuthorizationRequestBody(instruction),
                PaymentAuthorizationResponse.class,
                instruction.getHeaders(),
                "Authorize Payment"
        ).block(TIMEOUT);
    }

    /**
     * Calls the order payment endpoint
     * @param instruction Payment instruction containing the payment details
     * @return PaymentOrderResponse The order response
     */
    public PaymentOrderResponse callOrderPayment(PaymentInstruction instruction) {
        return executeRequest(
                properties.getOrder(),
                convertToOrderRequestBody(instruction),
                PaymentOrderResponse.class,
                instruction.getHeaders(),
                "Order Payment"
        ).block(TIMEOUT);
    }

    /**
     * Generic method to execute HTTP requests
     * @param uri The endpoint URI
     * @param requestBody The request body
     * @param responseType The expected response type
     * @param headers HTTP headers
     * @param operationType Operation type for logging
     * @return Mono<R> The response
     */
    private <T, R> Mono<R> executeRequest(String uri, T requestBody, Class<R> responseType,
                                          Map<String, String> headers, String operationType) {
        return webClient.post()
                .uri(uri)
                .headers(httpHeaders -> headers.forEach(httpHeaders::add))
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        handleClientError(response, headers.get(CORRELATION_ID)))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        handleServerError(response, headers.get(CORRELATION_ID)))
                .bodyToMono(responseType)
                .doOnSubscribe(subscription ->
                        log.debug("Initiating {} request with correlationId: {}",
                                operationType, headers.get(CORRELATION_ID)))
                .doOnNext(response ->
                        log.info("Received {} Response: {} with correlationId: {}",
                                operationType, response, headers.get(CORRELATION_ID)))
                .doOnError(error ->
                        log.error("Error during {} API call with correlationId {}: {}",
                                operationType, headers.get(CORRELATION_ID), error.getMessage(), error));
    }

    /**
     * Handles server errors (5xx)
     */
    private Mono<? extends Throwable> handleServerError(ClientResponse response, String correlationId) {
        return response.bodyToMono(String.class)
                .map(errorBody -> buildErrorMessage(response, correlationId, errorBody))
                .flatMap(errorMessage -> createServerException(response.statusCode().value(), errorMessage));
    }

    /**
     * Handles client errors (4xx)
     */
    private Mono<? extends Throwable> handleClientError(ClientResponse response, String correlationId) {
        return response.bodyToMono(String.class)
                .map(errorBody -> buildErrorMessage(response, correlationId, errorBody))
                .flatMap(errorMessage -> createClientException(response.statusCode().value(), errorMessage));
    }

    /**
     * Builds error message with consistent format
     */
    private String buildErrorMessage(ClientResponse response, String correlationId, String errorBody) {
        return String.format("%s error occurred. Status: %s, CorrelationId: %s, Body: %s",
                response.statusCode().is4xxClientError() ? "Client" : "Server",
                response.statusCode(), correlationId, errorBody);
    }

    /**
     * Creates appropriate server exception based on status code
     */
    private Mono<? extends Throwable> createServerException(int statusCode, String errorMessage) {
        log.error(errorMessage);
        return Mono.error(switch (statusCode) {
            case 502 -> new PaymentGatewayException(errorMessage);
            case 503 -> new PaymentServiceUnavailableException(errorMessage);
            default -> new PaymentServerException(errorMessage);
        });
    }

    /**
     * Creates appropriate client exception based on status code
     */
    private Mono<? extends Throwable> createClientException(int statusCode, String errorMessage) {
        log.error(errorMessage);
        return Mono.error(switch (statusCode) {
            case 400 -> new PaymentBadRequestException(errorMessage);
            case 401 -> new PaymentUnauthorizedException(errorMessage);
            case 403 -> new PaymentForbiddenException(errorMessage);
            case 404 -> new PaymentNotFoundException(errorMessage);
            case 409 -> new PaymentConflictException(errorMessage);
            default -> new PaymentClientException(errorMessage);
        });
    }

    /**
     * Converts payment instruction to authorization request
     */
    private PaymentAuthorizationRequest convertToAuthorizationRequestBody(PaymentInstruction instruction) {
        return PaymentAuthorizationRequest.builder()
                .method(AUTHORIZE_PAYMENT_METHOD)
                .paymentId(instruction.getPaymentId())
                .debtor(instruction.getDebtor())
                .creditor(instruction.getCreditor())
                .amount(instruction.getAmount().doubleValue())
                .currency(instruction.getCurrency())
                .paymentReference(instruction.getPaymentReference())
                .paymentDate(instruction.getPaymentDate())
                .build();
    }

    /**
     * Converts payment instruction to order request
     */
    private PaymentOrderRequest convertToOrderRequestBody(PaymentInstruction instruction) {
        return PaymentOrderRequest.builder()
                .paymentId(instruction.getPaymentId())
                .debtor(instruction.getDebtor())
                .creditor(instruction.getCreditor())
                .amount(instruction.getAmount().doubleValue())
                .currency(instruction.getCurrency())
                .paymentReference(instruction.getPaymentReference())
                .paymentDate(instruction.getPaymentDate())
                .priority(NORMAL_PRIORITY)
                .build();
    }
}

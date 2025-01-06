package com.payments.simulation;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.UUID;

public class PaymentSimulation extends Simulation {

    private final HttpProtocolBuilder httpProtocol;
    private final ScenarioBuilder scn;

    public PaymentSimulation() {
        httpProtocol = http
                .baseUrl("http://localhost:8081") // Base URL for the application
                .acceptHeader("application/json")
                .contentTypeHeader("application/json");

        scn = scenario("PaymentSimulation")
                .exec(session -> {
                    // Add a random idempotency key to the session
                    String idempotencyKey = UUID.randomUUID().toString();
                    return session.set("idempotencyKey", idempotencyKey);
                })
                .exec(
                        http("Submit Payment")
                                .post("/api/payments/v1/submit-payment")
                                .header("x-correlation-id", "#{randomUuid()}")
                                .header("x-idempotency-key", "#{idempotencyKey}") // Use the dynamically generated key
                                .body(StringBody("{ \"debtor\": { \"accountNumber\": \"123456789\", \"accountName\": \"John Doe\" }, \"creditor\": { \"accountNumber\": \"123456786\", \"accountName\": \"John Doe\" }, \"amount\": 100.5, \"currency\": \"USD\", \"paymentReference\": \"#{idempotencyKey}\", \"paymentDate\": \"2023-10-01\" }"))
                                .check(status().is(201))
                                .check(responseTimeInMillis().lt(500))
                );

        setUp(
                scn.injectOpen(constantUsersPerSec(10).during(20)) // Inject 10 users at once
        ).protocols(httpProtocol);
    }
}
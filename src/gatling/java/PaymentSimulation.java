
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

public class PaymentSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8080") // Base URL for the application
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");


    ScenarioBuilder scn = scenario("PaymentSimulation")
            .exec(

                    http("Submit Payment")
                            .post("/api/payments/v1/submit-payment")
                            .header("x-correlation-id", "#{randomUuid()}")
                            .header("x-idempotency-key", "INV123456")
                            .body(StringBody("{ \"debtor\": { \"accountNumber\": \"123456789\", \"accountName\": \"John Doe\" }, \"creditor\": { \"accountNumber\": \"123456789\", \"accountName\": \"John Doe\" }, \"amount\": 100.5, \"currency\": \"USD\", \"paymentReference\": \"INV123456\", \"paymentDate\": \"2023-10-01\" }"))
                            .check(status().is(201))
            );

    {
        setUp(
                scn.injectOpen(constantUsersPerSec(10).during(20)) // Inject 10 users at once
        ).protocols(httpProtocol);
    }
}

package com.payments.frontdoor.web;

import com.payments.frontdoor.domain.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class PaymentController {

    @PostMapping("/submit-payment")
    public ResponseEntity<String> payment(@RequestBody PaymentRequest request) {
        log.info("Payment request received for card number: {}", request.getCardNumber());
        return ResponseEntity.ok("Payment successful");
    }
}

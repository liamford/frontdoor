package com.payments.frontdoor.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentDispacherService {

    private final KafkaProducer kafkaProducer;

    public void dispatchPayment(String message) {
        kafkaProducer.sendMessage(message);
    }
}

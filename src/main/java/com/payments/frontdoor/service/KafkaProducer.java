package com.payments.frontdoor.service;

import com.payments.avro.PaymentRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, PaymentRecord> kafkaTemplate;

    public void sendMessage(String topic, String key, PaymentRecord message) {
        ProducerRecord<String, PaymentRecord> producerRecord = new ProducerRecord<>(topic, key, message);
        CompletableFuture<SendResult<String, PaymentRecord>> completableFuture = kafkaTemplate.send(producerRecord);
        log.info("Sending kafka message on topic {}", topic);

        completableFuture.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Kafka message successfully sent on topic {} and Key {}", topic, result.getProducerRecord().key());
            } else {
                log.error("An error occurred while sending kafka message for event with key {}", key);
            }
        });
    }
}
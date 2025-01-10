package com.payments.frontdoor.service;


import com.payments.frontdoor.config.KafkaCustomProperties;
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

    private final KafkaCustomProperties kafkaCustomProperties;

    private final KafkaTemplate<String, String> kafkaTemplate;


    public void sendMessage(String message) {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(kafkaCustomProperties.getExecutionTopic(), message, message);
        CompletableFuture<SendResult<String, String>> completableFuture = kafkaTemplate.send(producerRecord);
        log.info("Sending kafka message on topic {}", kafkaCustomProperties.getExecutionTopic());

        completableFuture.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Kafka message successfully sent on topic {} and value {}", kafkaCustomProperties.getExecutionTopic(), result.getProducerRecord().value().toString());
            } else {
                log.error("An error occurred while sending kafka message for event with value {}", producerRecord);
            }
        });
    }
}

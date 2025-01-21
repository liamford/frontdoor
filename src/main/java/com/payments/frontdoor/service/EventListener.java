package com.payments.frontdoor.service;

import com.payments.frontdoor.activities.PaymentStepStatus;
import com.payments.frontdoor.avro.PaymentRecord;
import com.payments.frontdoor.exception.PaymentProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class EventListener {

    private static final String EXECUTED_TOPIC = "${payments.kafka.topics.executed}";
    private static final String POSTED_TOPIC = "${payments.kafka.topics.posted}";
    private static final String KAFKA_GROUP_ID = "${spring.kafka.consumer.group-id}";
    private static final String CONTAINER_FACTORY = "kafkaListenerContainerFactory";

    private final PaymentProcessService paymentProcessService;

    @KafkaListener(
            topics = EXECUTED_TOPIC,
            containerFactory = CONTAINER_FACTORY,
            groupId = KAFKA_GROUP_ID
    )
    public void listenSignal(ConsumerRecord<String, PaymentRecord> record, Acknowledgment ack) {
        String key = record.key();
        log.info("Processing executed payment event - key: {}", key);

        try {
            paymentProcessService.sendSignal(PaymentStepStatus.EXECUTED, key);
            log.info("Successfully processed executed payment - key: {}", key);
        } catch (Exception e) {
            log.error("Failed to process executed payment - key: {} - error: {}", key, e.getMessage());
            throw e;
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = POSTED_TOPIC,
            containerFactory = CONTAINER_FACTORY,
            groupId = KAFKA_GROUP_ID
    )
    public void listenToken(ConsumerRecord<String, PaymentRecord> record, Acknowledgment ack) {
        String key = record.key();
        log.info("Processing posted payment event - key: {}", key);

        try {
            byte[] token = extractToken(record.value());
            paymentProcessService.sendToken(token);
            log.info("Successfully processed posted payment - key: {}", key);
        } catch (Exception e) {
            log.error("Failed to process posted payment - key: {} - error: {}", key, e.getMessage());
            throw e;
        } finally {
            ack.acknowledge();
        }
    }

    private byte[] extractToken(PaymentRecord record) {
        return Optional.ofNullable(record)
                .map(PaymentRecord::getToken)
                .filter(buffer -> buffer != null && buffer.hasRemaining())
                .map(this::bufferToBytes)
                .orElseThrow(() -> new PaymentProcessingException("Invalid or missing token"));
    }

    private byte[] bufferToBytes(ByteBuffer buffer) {
        byte[] token = new byte[buffer.remaining()];
        buffer.get(token);
        return token;
    }
}

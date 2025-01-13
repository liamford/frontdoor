package com.payments.frontdoor.service;

import com.payments.avro.PaymentRecord;
import com.payments.frontdoor.activities.PaymentStepStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class EventListener {

    private final PaymentProcessService paymentProcessService;

    @KafkaListener(topics = "${payments.kafka.topics.executed}", containerFactory = "kafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}")
    public void listen(ConsumerRecord<String, PaymentRecord> transactionEventMessage, Acknowledgment  acknowledgment) {
        log.info("Starting consuming from payments.executed.v1 - {}", transactionEventMessage.key());
        paymentProcessService.sendSignal(PaymentStepStatus.EXECUTED, transactionEventMessage.key());
        acknowledgment.acknowledge();
    }
}

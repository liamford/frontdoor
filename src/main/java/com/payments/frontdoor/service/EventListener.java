package com.payments.frontdoor.service;


import com.payments.frontdoor.activities.PaymentStepStatus;
import com.payments.frontdoor.avro.PaymentRecord;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;

@Slf4j
@Service
@AllArgsConstructor
public class EventListener {

    private final PaymentProcessService paymentProcessService;

    @KafkaListener(topics = "${payments.kafka.topics.executed}", containerFactory = "kafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}")
    public void listenSignal(ConsumerRecord<String, PaymentRecord> transactionEventMessage, Acknowledgment  acknowledgment) {
        log.info("Starting consuming from payments.executed.v1 - {}", transactionEventMessage.key());
        paymentProcessService.sendSignal(PaymentStepStatus.EXECUTED, transactionEventMessage.key());
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = "${payments.kafka.topics.posted}", containerFactory = "kafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}")
    public void listenToken(ConsumerRecord<String, PaymentRecord> transactionEventMessage, Acknowledgment  acknowledgment) {
        log.info("Starting consuming from payments.posted.v1 - {}", transactionEventMessage.key());
          try {
              byte[] token = getTokenFromEvent(transactionEventMessage.value().getToken());
              paymentProcessService.sendToken(token);
          } catch(Exception e) {
              throw new IllegalArgumentException("token is not valid");
          } finally {
              acknowledgment.acknowledge();
          }

    }

    private byte[] getTokenFromEvent(ByteBuffer byteBuffer) {
        byte[] token = new byte[byteBuffer.remaining()];
        byteBuffer.get(token);
        return token;
    }

}

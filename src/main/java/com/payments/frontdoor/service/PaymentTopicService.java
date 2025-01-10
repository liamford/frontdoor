package com.payments.frontdoor.service;

import com.payments.frontdoor.activities.PaymentStepStatus;
import com.payments.frontdoor.config.KafkaCustomProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentTopicService {

    private final KafkaCustomProperties kafkaCustomProperties;
    private final Map<PaymentStepStatus, String> topicMap = new EnumMap<>(PaymentStepStatus.class);

    @PostConstruct
    public void init() {
        topicMap.put(PaymentStepStatus.INITIATED, kafkaCustomProperties.getInitiated());
        topicMap.put(PaymentStepStatus.MANAGED, kafkaCustomProperties.getManaged());
        topicMap.put(PaymentStepStatus.AUTHORIZED, kafkaCustomProperties.getAuthorized());
        topicMap.put(PaymentStepStatus.EXECUTED, kafkaCustomProperties.getExecuted());
        topicMap.put(PaymentStepStatus.CLEARED, kafkaCustomProperties.getCleared());
        topicMap.put(PaymentStepStatus.NOTIFIED, kafkaCustomProperties.getNotified());
        topicMap.put(PaymentStepStatus.RECONCILED, kafkaCustomProperties.getReconciled());
        topicMap.put(PaymentStepStatus.POSTED, kafkaCustomProperties.getPosted());
        topicMap.put(PaymentStepStatus.REFUND, kafkaCustomProperties.getRefund());
        topicMap.put(PaymentStepStatus.REPORTED, kafkaCustomProperties.getReported());
        topicMap.put(PaymentStepStatus.ARCHIVED, kafkaCustomProperties.getArchived());
    }

    public String getTopicName(PaymentStepStatus status) {
        return topicMap.get(status);
    }
}
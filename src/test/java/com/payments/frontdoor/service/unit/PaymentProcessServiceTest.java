package com.payments.frontdoor.service.unit;


import com.payments.frontdoor.activities.PaymentStepStatus;
import com.payments.frontdoor.config.KafkaCustomProperties;
import com.payments.frontdoor.service.PaymentTopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaymentTopicServiceTest {

    @Mock
    private KafkaCustomProperties kafkaCustomProperties;

    private PaymentTopicService paymentTopicService;

    @BeforeEach
    void setUp() {
        // Arrange
        when(kafkaCustomProperties.getInitiated()).thenReturn("payment.initiated");
        when(kafkaCustomProperties.getManaged()).thenReturn("payment.managed");
        when(kafkaCustomProperties.getAuthorized()).thenReturn("payment.authorized");
        when(kafkaCustomProperties.getExecuted()).thenReturn("payment.executed");
        when(kafkaCustomProperties.getCleared()).thenReturn("payment.cleared");
        when(kafkaCustomProperties.getNotified()).thenReturn("payment.notified");
        when(kafkaCustomProperties.getReconciled()).thenReturn("payment.reconciled");
        when(kafkaCustomProperties.getPosted()).thenReturn("payment.posted");
        when(kafkaCustomProperties.getRefund()).thenReturn("payment.refund");
        when(kafkaCustomProperties.getReported()).thenReturn("payment.reported");
        when(kafkaCustomProperties.getArchived()).thenReturn("payment.archived");

        paymentTopicService = new PaymentTopicService(kafkaCustomProperties);
        paymentTopicService.init();
    }

    @Test
    void getTopicName_ShouldReturnCorrectTopicForEachStatus() {
        // Assert
        assertEquals("payment.initiated", paymentTopicService.getTopicName(PaymentStepStatus.INITIATED));
        assertEquals("payment.managed", paymentTopicService.getTopicName(PaymentStepStatus.MANAGED));
        assertEquals("payment.authorized", paymentTopicService.getTopicName(PaymentStepStatus.AUTHORIZED));
        assertEquals("payment.executed", paymentTopicService.getTopicName(PaymentStepStatus.EXECUTED));
        assertEquals("payment.cleared", paymentTopicService.getTopicName(PaymentStepStatus.CLEARED));
        assertEquals("payment.notified", paymentTopicService.getTopicName(PaymentStepStatus.NOTIFIED));
        assertEquals("payment.reconciled", paymentTopicService.getTopicName(PaymentStepStatus.RECONCILED));
        assertEquals("payment.posted", paymentTopicService.getTopicName(PaymentStepStatus.POSTED));
        assertEquals("payment.refund", paymentTopicService.getTopicName(PaymentStepStatus.REFUND));
        assertEquals("payment.reported", paymentTopicService.getTopicName(PaymentStepStatus.REPORTED));
        assertEquals("payment.archived", paymentTopicService.getTopicName(PaymentStepStatus.ARCHIVED));
    }

    @Test
    void getTopicName_WithNullStatus_ShouldReturnNull() {
        // Act
        String result = paymentTopicService.getTopicName(null);

        // Assert
        assertNull(result);
    }

    @Test
    void init_ShouldPopulateAllTopics() {
        // Arrange
        PaymentTopicService newService = new PaymentTopicService(kafkaCustomProperties);

        // Act
        newService.init();

        // Assert
        for (PaymentStepStatus status : PaymentStepStatus.values()) {
            assertNotNull(newService.getTopicName(status),
                "Topic should not be null for status: " + status);
        }
    }

    @Test
    void getTopicName_ShouldReturnConsistentResults() {
        // Act & Assert
        String topic1 = paymentTopicService.getTopicName(PaymentStepStatus.INITIATED);
        String topic2 = paymentTopicService.getTopicName(PaymentStepStatus.INITIATED);

        assertEquals(topic1, topic2, "Multiple calls should return the same topic");
    }

    @Test
    void topicNames_ShouldBeUnique() {
        // Arrange
        PaymentStepStatus[] statuses = PaymentStepStatus.values();
        String[] topics = new String[statuses.length];

        // Act
        for (int i = 0; i < statuses.length; i++) {
            topics[i] = paymentTopicService.getTopicName(statuses[i]);
        }

        // Assert
        for (int i = 0; i < topics.length; i++) {
            for (int j = i + 1; j < topics.length; j++) {
                assertNotEquals(topics[i], topics[j],
                    String.format("Topics should be unique: %s and %s have the same topic",
                        statuses[i], statuses[j]));
            }
        }
    }

    @Test
    void customProperties_ShouldNotBeNull() {
        // Assert
        assertNotNull(kafkaCustomProperties, "KafkaCustomProperties should not be null");
    }
}


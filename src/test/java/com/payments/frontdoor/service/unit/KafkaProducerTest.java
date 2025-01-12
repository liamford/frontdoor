package com.payments.frontdoor.service.unit;


import com.payments.avro.PaymentRecord;
import com.payments.frontdoor.service.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class KafkaProducerTest {

    @Mock
    private KafkaTemplate<String, PaymentRecord> kafkaTemplate;

    private KafkaProducer kafkaProducer;

    @BeforeEach
    void setUp() {
        kafkaProducer = new KafkaProducer(kafkaTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendMessage_WhenSuccessful_ShouldReturnCompletedFuture() {
        // Arrange
        String topic = "test-topic";
        String key = "test-key";
        PaymentRecord paymentRecord = mock(PaymentRecord.class);

        SendResult<String, PaymentRecord> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, PaymentRecord>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // Act
        CompletableFuture<SendResult<String, PaymentRecord>> result =
                kafkaProducer.sendMessage(topic, key, paymentRecord);

        // Assert
        assertNotNull(result);
        verify(kafkaTemplate).send(new ProducerRecord<>(topic, key, paymentRecord));
    }


    @Test
    @SuppressWarnings("unchecked")
    void sendMessage_WhenFailure_ShouldHandleException() {
        // Arrange
        String topic = "test-topic";
        String key = "test-key";
        PaymentRecord paymentRecord = mock(PaymentRecord.class);

        RuntimeException exception = new RuntimeException("Failed to send message");
        CompletableFuture<SendResult<String, PaymentRecord>> future = CompletableFuture.failedFuture(exception);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // Act
        CompletableFuture<SendResult<String, PaymentRecord>> result = kafkaProducer.sendMessage(topic, key, paymentRecord);

        // Assert
        assertNotNull(result);
        assertTrue(result.isCompletedExceptionally());
    }

    @Test
    void sendMessage_WhenNullParameters_ShouldThrowException() {
        // Arrange
        String topic = "test-topic";
        String key = "test-key";
        PaymentRecord paymentRecord = mock(PaymentRecord.class);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                        kafkaProducer.sendMessage(null, key, paymentRecord),
                "Topic should not be null"
        );

        assertThrows(NullPointerException.class, () ->
                        kafkaProducer.sendMessage(topic, key, null),
                "Message should not be null"
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendMessage_ShouldCompleteWithCallback() {
        // Arrange
        String topic = "test-topic";
        String key = "test-key";
        PaymentRecord paymentRecord = mock(PaymentRecord.class);

        ProducerRecord<String, PaymentRecord> expectedRecord = new ProducerRecord<>(topic, key, paymentRecord);
        SendResult<String, PaymentRecord> sendResult = mock(SendResult.class);
        when(sendResult.getProducerRecord()).thenReturn(expectedRecord);

        CompletableFuture<SendResult<String, PaymentRecord>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // Act
        CompletableFuture<SendResult<String, PaymentRecord>> result = kafkaProducer.sendMessage(topic, key, paymentRecord);

        // Assert
        assertNotNull(result);
        assertFalse(result.isDone());

        // Complete the future
        future.complete(sendResult);

        assertTrue(result.isDone());
        assertFalse(result.isCompletedExceptionally());
    }
}

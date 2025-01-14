package com.payments.frontdoor.service.unit;



import com.payments.frontdoor.activities.PaymentStepStatus;
import com.payments.frontdoor.avro.Account;
import com.payments.frontdoor.avro.PaymentRecord;
import com.payments.frontdoor.service.KafkaProducer;
import com.payments.frontdoor.service.PaymentDispatcherService;
import com.payments.frontdoor.service.PaymentTopicService;
import com.payments.frontdoor.model.PaymentInstruction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentDispatcherServiceTest {

    @Mock
    private KafkaProducer kafkaProducer;

    @Mock
    private PaymentTopicService paymentTopicService;

    private PaymentDispatcherService paymentDispatcherService;

    @BeforeEach
    void setUp() {
        paymentDispatcherService = new PaymentDispatcherService(kafkaProducer, paymentTopicService);
    }

    @Test
    void dispatchPayment_ShouldSendKafkaMessage() {
        // Arrange
        PaymentInstruction instruction = createSamplePaymentInstruction();
        PaymentStepStatus status = PaymentStepStatus.INITIATED;
        String expectedTopic = "payment.initiated";

        when(paymentTopicService.getTopicName(status)).thenReturn(expectedTopic);

        // Act
        paymentDispatcherService.dispatchPayment(instruction, status, null);

        // Assert
        verify(paymentTopicService).getTopicName(status);
        verify(kafkaProducer).sendMessage(
                eq(expectedTopic),
                eq(instruction.getPaymentId()),
                argThat(record -> validatePaymentRecord(record, instruction))
        );
    }

    private boolean validatePaymentRecord(PaymentRecord record, PaymentInstruction instruction) {
        return record.getPaymentId().equals(instruction.getPaymentId()) &&
                Double.compare(record.getAmount(), instruction.getAmount().doubleValue()) == 0 &&
                record.getCurrency().equals(instruction.getCurrency()) &&
                record.getPaymentReference().equals(instruction.getPaymentReference()) &&
                record.getPaymentDate().equals(instruction.getPaymentDate().toString()) &&
                record.getBankAddress().equals(instruction.getBankAddress()) &&
                record.getBankCountry().equals(instruction.getBankCountry()) &&
                record.getBankCity().equals(instruction.getBankCity()) &&
                record.getBic().equals(instruction.getBic()) &&
                record.getBankName().equals(instruction.getBankName()) &&
                record.getPaymentStatus().equals(instruction.getPaymentStatus()) &&
                validateAccount(record.getDebtor(), instruction.getDebtor()) &&
                validateAccount(record.getCreditor(), instruction.getCreditor());
    }

    private boolean validateAccount(Account actual, com.payments.frontdoor.swagger.model.Account expected) {
        return actual.getAccountName().equals(expected.getAccountName()) &&
                actual.getAccountNumber().equals(expected.getAccountNumber());
    }

    private PaymentInstruction createSamplePaymentInstruction() {
        com.payments.frontdoor.swagger.model.Account debtor = new com.payments.frontdoor.swagger.model.Account();
        debtor.setAccountName("Debtor Name");
        debtor.setAccountNumber("DE123456789");

        com.payments.frontdoor.swagger.model.Account creditor = new com.payments.frontdoor.swagger.model.Account();
        creditor.setAccountName("Creditor Name");
        creditor.setAccountNumber("GB987654321");

        PaymentInstruction instruction = new PaymentInstruction();
        instruction.setPaymentId("PAY123");
        instruction.setAmount(BigDecimal.valueOf(1000.00));
        instruction.setCurrency("EUR");
        instruction.setPaymentReference("REF123");
        instruction.setPaymentDate(LocalDate.now());
        instruction.setBankAddress("123 Bank Street");
        instruction.setBankCountry("DE");
        instruction.setBankCity("Berlin");
        instruction.setBic("DEUTDEFF");
        instruction.setBankName("Deutsche Bank");
        instruction.setPaymentStatus("PENDING");
        instruction.setDebtor(debtor);
        instruction.setCreditor(creditor);

        return instruction;
    }
}


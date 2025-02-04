package com.payments.frontdoor.service;


import com.payments.frontdoor.activities.PaymentStepStatus;
import com.payments.frontdoor.avro.Account;
import com.payments.frontdoor.avro.PaymentRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.payments.frontdoor.model.PaymentInstruction;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentDispatcherService {

    private final KafkaProducer kafkaProducer;
    private final PaymentTopicService paymentTopicService;

    public void dispatchPayment(PaymentInstruction instruction, PaymentStepStatus status, byte[] taskToken) {
        String topic = paymentTopicService.getTopicName(status);
        PaymentRecord paymentRecord = convertToPaymentRecord(instruction);
        if(taskToken != null) {
            paymentRecord.setToken(ByteBuffer.wrap(taskToken));
        }
        kafkaProducer.sendMessage(topic, instruction.getPaymentId(), paymentRecord);
    }

    private PaymentRecord convertToPaymentRecord(PaymentInstruction instruction) {
        return PaymentRecord.newBuilder()
                .setPaymentId(instruction.getPaymentId())
                .setAmount(instruction.getAmount().doubleValue())
                .setCurrency(instruction.getCurrency())
                .setPaymentReference(instruction.getPaymentReference())
                .setPaymentDate(instruction.getPaymentDate().toString())
                .setBankAddress(instruction.getBankAddress())
                .setBankCountry(instruction.getBankCountry())
                .setBankCity(instruction.getBankCity())
                .setBic(instruction.getBic())
                .setBankName(instruction.getBankName())
                .setPaymentStatus(instruction.getPaymentStatus())
                .setDebtor(Account.newBuilder()
                        .setAccountName(instruction.getDebtor().getAccountName())
                        .setAccountNumber(instruction.getDebtor().getAccountNumber())
                        .build())
                .setCreditor(Account.newBuilder()
                        .setAccountName(instruction.getCreditor().getAccountName())
                        .setAccountNumber(instruction.getCreditor().getAccountNumber())
                        .build())
                .build();
    }
}
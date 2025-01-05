package com.payments.frontdoor.activities;

import com.payments.frontdoor.swagger.model.PaymentResponse.StatusEnum;
import io.temporal.spring.boot.ActivityImpl;
import lombok.extern.slf4j.Slf4j;
import model.PaymentDetails;
import model.PaymentInstruction;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ActivityImpl(workers = "send-payment-worker")
public class AccountActivityImpl implements AccountActivity {

    @Override
    public PaymentInstruction initiatePayment(PaymentDetails input) {
        log.info("Initiating payment for: {}", input);
        return convertToPaymentInstruction(input);
    }

    @Override
    public boolean managePaymentOrder(PaymentInstruction instruction) {
        log.info("Managing payment order for: {}", instruction);
        return true; // Assume order is valid
    }

    @Override
    public boolean authorizePayment(PaymentInstruction instruction) {
        log.info("Authorizing payment: {}", instruction);
        return true; // Assume payment is authorized
    }

    @Override
    public StatusEnum executePayment(PaymentInstruction instruction) {
        log.info("Executing payment: {}", instruction);
        return StatusEnum.ACSP;
    }

    @Override
    public StatusEnum clearAndSettlePayment(PaymentInstruction instruction) {
        log.info("Clearing and settling payment: {}", instruction);
        return StatusEnum.ACSC;
    }

    @Override
    public StatusEnum sendNotification(PaymentInstruction instruction) {
        log.info("Sending notification for payment: {}", instruction);
        return StatusEnum.ACSP;
    }

    @Override
    public StatusEnum reconcilePayment(PaymentInstruction instruction) {
        log.info("Reconciling payment: {}", instruction);
        return StatusEnum.ACSP;
    }

    @Override
    public StatusEnum postPayment(PaymentInstruction instruction) {
        log.info("Posting payment to ledger: {}", instruction);
        if (instruction.getDebtor().equals(instruction.getCreditor())) {
            throw new IllegalArgumentException("Debtor and creditor accounts are same");
        }
        return StatusEnum.ACSP;
    }

    @Override
    public StatusEnum generateReports(PaymentInstruction instruction) {
        log.info("Generating reports for payment: {}", instruction);
        return StatusEnum.ACSP;
    }

    @Override
    public StatusEnum archivePayment(PaymentInstruction instruction) {
        log.info("Archiving payment: {}", instruction);
        return StatusEnum.ACSP;
    }

    @Override
    public StatusEnum refundPayment(PaymentInstruction instruction) {
        log.info("Refunding payment: {}", instruction);
        return StatusEnum.RJCT;
    }

    private PaymentInstruction convertToPaymentInstruction(PaymentDetails details) {
        return PaymentInstruction.builder()
                .paymentStatus(details.getPaymentStatus())
                .paymentId(details.getPaymentId())
                .debtor(details.getDebtor())
                .creditor(details.getCreditor())
                .amount(details.getAmount())
                .currency(details.getCurrency())
                .paymentReference(details.getPaymentReference())
                .paymentDate(details.getPaymentDate())
                .bankAddress("Main Road")
                .bankCountry("Australia")
                .bankCity("Melbourne")
                .bic("LIAM123")
                .bankName("Liam Bank")
                .build();
    }
}
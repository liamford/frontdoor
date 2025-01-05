package com.payments.frontdoor.activities;

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
    public String executePayment(PaymentInstruction instruction) {
        log.info("Executing payment: {}", instruction);
        return ActivityStepStatus.EXECUTED.getStatus();
    }

    @Override
    public String clearAndSettlePayment(PaymentInstruction instruction) {
        log.info("Clearing and settling payment: {}", instruction);
        return ActivityStepStatus.CLEARED.getStatus();
    }

    @Override
    public String sendNotification(PaymentInstruction instruction) {
        log.info("Sending notification for payment: {}", instruction);
        return ActivityStepStatus.NOTIFIED.getStatus();
    }

    @Override
    public String reconcilePayment(PaymentInstruction instruction) {
        log.info("Reconciling payment: {}", instruction);
        return ActivityStepStatus.RECONCILED.getStatus();
    }

    @Override
    public String postPayment(PaymentInstruction instruction) {
        log.info("Posting payment to ledger: {}", instruction);
        if (instruction.getDebtor().equals(instruction.getCreditor())) {
            throw new IllegalArgumentException("Debtor and creditor accounts are same");
        }
        return ActivityStepStatus.POSTED.getStatus();
    }

    @Override
    public String generateReports(PaymentInstruction instruction) {
        log.info("Generating reports for payment: {}", instruction);
        return ActivityStepStatus.REPORTED.getStatus();
    }

    @Override
    public String archivePayment(PaymentInstruction instruction) {
        log.info("Archiving payment: {}", instruction);
        return ActivityStepStatus.ARCHIVED.getStatus();
    }

    @Override
    public String refundPayment(PaymentInstruction instruction) {
        log.info("Refunding payment: {}", instruction);
        return ActivityStepStatus.REFUND.getStatus();
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
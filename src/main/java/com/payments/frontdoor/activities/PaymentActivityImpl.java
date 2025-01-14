package com.payments.frontdoor.activities;

import com.payments.frontdoor.model.PaymentDetails;
import com.payments.frontdoor.model.PaymentInstruction;
import com.payments.frontdoor.service.PaymentDispatcherService;
import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.spring.boot.ActivityImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ForkJoinPool;

@Slf4j
@Component
@ActivityImpl(workers = "send-payment-worker")
public class PaymentActivityImpl implements PaymentActivity {



    private final PaymentDispatcherService dispatcherService;

    public PaymentActivityImpl(PaymentDispatcherService dispatcherService) {
        this.dispatcherService = dispatcherService;
    }


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
    public PaymentStepStatus executePayment(PaymentInstruction instruction) {
        log.info("Executing payment: {}", instruction);
        dispatcherService.dispatchPayment(instruction, PaymentStepStatus.EXECUTED, null);
        return PaymentStepStatus.EXECUTED;
    }

    @Override
    public PaymentStepStatus clearAndSettlePayment(PaymentInstruction instruction) {
        log.info("Clearing and settling payment: {}", instruction);
        dispatcherService.dispatchPayment(instruction, PaymentStepStatus.CLEARED, null);

        return PaymentStepStatus.CLEARED;
    }

    @Override
    public PaymentStepStatus sendNotification(PaymentInstruction instruction) {
        log.info("Sending notification for payment: {}", instruction);
        dispatcherService.dispatchPayment(instruction, PaymentStepStatus.NOTIFIED, null);
        return PaymentStepStatus.NOTIFIED;
    }

    @Override
    public PaymentStepStatus reconcilePayment(PaymentInstruction instruction) {
        log.info("Reconciling payment: {}", instruction);
        dispatcherService.dispatchPayment(instruction, PaymentStepStatus.RECONCILED, null);
        return PaymentStepStatus.RECONCILED;
    }

    @Override
    public PaymentStepStatus postPayment(PaymentInstruction instruction) {
        log.info("Posting payment to ledger: {}", instruction);
        if (instruction.getDebtor().equals(instruction.getCreditor())) {
            throw new IllegalArgumentException("Debtor and creditor accounts are same");
        }
        ActivityExecutionContext context = Activity.getExecutionContext();
        byte[] taskToken = context.getTaskToken();

        ForkJoinPool.commonPool().execute(() ->
                dispatcherService.dispatchPayment(instruction, PaymentStepStatus.POSTED, taskToken));
        context.doNotCompleteOnReturn();
        return PaymentStepStatus.POSTED;
    }

    @Override
    public PaymentStepStatus generateReports(PaymentInstruction instruction) {
        log.info("Generating reports for payment: {}", instruction);
        dispatcherService.dispatchPayment(instruction, PaymentStepStatus.REPORTED, null);

        return PaymentStepStatus.REPORTED;
    }

    @Override
    public PaymentStepStatus archivePayment(PaymentInstruction instruction) {
        log.info("Archiving payment: {}", instruction);
        dispatcherService.dispatchPayment(instruction, PaymentStepStatus.ARCHIVED, null);

        return PaymentStepStatus.ARCHIVED;
    }

    @Override
    public PaymentStepStatus refundPayment(PaymentInstruction instruction) {
        log.info("Refunding payment: {}", instruction);
        dispatcherService.dispatchPayment(instruction, PaymentStepStatus.REFUND, null);

        return PaymentStepStatus.REFUND;
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
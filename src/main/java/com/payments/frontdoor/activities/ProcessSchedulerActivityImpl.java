package com.payments.frontdoor.activities;

import com.payments.frontdoor.model.PaymentDetails;
import com.payments.frontdoor.service.PaymentProcessService;
import io.temporal.spring.boot.ActivityImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@ActivityImpl(workers = {"normal-payment-worker"})
public class ProcessSchedulerActivityImpl implements ProcessSchedulerActivity {

    private final PaymentProcessService paymentProcessService;

    public ProcessSchedulerActivityImpl(PaymentProcessService paymentProcessService) {
        this.paymentProcessService = paymentProcessService;
    }

    @Override
    public String submitPayments(PaymentDetails input) {
        log.info("Submitting payment for: {}", input);
         paymentProcessService.processPaymentAsync(input);
        return "Payment submitted successfully";
    }

    @Override
    public String healthCheck() {

        log.info("Health check for ProcessSchedulerActivity");
        return "ProcessSchedulerActivity is healthy";
    }

    @Override
    public String notifyBatchJobStatus(String paymentType) {
        log.info("Notifying batch job status for {}", paymentType);
        return "Batch job status for " + paymentType + " is updated";
    }
}

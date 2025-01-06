package com.payments.frontdoor.service;

import com.payments.frontdoor.config.TemporalWorkflowConfig;
import com.payments.frontdoor.workflows.PaymentWorkflow;
import io.temporal.client.WorkflowClient;
import lombok.AllArgsConstructor;
import model.PaymentDetails;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PaymentProcessService {

    private final WorkflowClient workflowClient;
    private final TemporalWorkflowConfig temporalWorkflowConfig;

    @Async
    public void processPaymentAsync(PaymentDetails paymentDetails, String workflowId) {
        PaymentWorkflow workflow = temporalWorkflowConfig.sendPaymentWorkflowWithId(workflowClient, workflowId);
        WorkflowClient.start(workflow::processPayment, paymentDetails);
    }
}
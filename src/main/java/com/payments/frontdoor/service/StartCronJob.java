package com.payments.frontdoor.service;

import com.payments.frontdoor.config.TemporalWorkflowConfig;
import com.payments.frontdoor.util.PaymentUtil;
import com.payments.frontdoor.workflows.ProcessScheduler;
import io.temporal.client.WorkflowClient;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class StartCronJob {

    private final WorkflowClient workflowClient;
    private final TemporalWorkflowConfig temporalWorkflowConfig;

    public StartCronJob(WorkflowClient workflowClient, TemporalWorkflowConfig temporalWorkflowConfig) {
        this.workflowClient = workflowClient;
        this.temporalWorkflowConfig = temporalWorkflowConfig;
    }

    @PostConstruct
    public void startCronWorkflow() {

        ProcessScheduler processScheduler = temporalWorkflowConfig.processSchedulerWithId(
            workflowClient,
            "Domestic-Payments_" +
            PaymentUtil.getCurrentDateTime()
        );
        WorkflowClient.start(processScheduler::batchPaymentProcessor, "Domestic-Payments");
    }

}

package com.payments.frontdoor.config;

import com.payments.frontdoor.workflows.PaymentWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalWorkflowConfig {

    @Bean
    public WorkflowOptions workflowOptions() {
        return WorkflowOptions.newBuilder()
                .setTaskQueue("payment_subscription")
                .build();
    }

    @Bean
    public PaymentWorkflow sendPaymentWorkflow(WorkflowClient workflowClient, WorkflowOptions workflowOptions) {
        return workflowClient.newWorkflowStub(PaymentWorkflow.class, workflowOptions);
    }

    public PaymentWorkflow sendPaymentWorkflowWithId(WorkflowClient workflowClient, String workflowId) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue("payment_subscription")
                .setWorkflowId(workflowId)
                .build();
        return workflowClient.newWorkflowStub(PaymentWorkflow.class, options);
    }
}
package com.payments.frontdoor.config;

import com.payments.frontdoor.workflows.SendPaymentWorkflow;
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
    public SendPaymentWorkflow sendPaymentWorkflow(WorkflowClient workflowClient, WorkflowOptions workflowOptions) {
        return workflowClient.newWorkflowStub(SendPaymentWorkflow.class, workflowOptions);
    }

    public SendPaymentWorkflow sendPaymentWorkflowWithId(WorkflowClient workflowClient, String workflowId) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue("payment_subscription")
                .setWorkflowId(workflowId)
                .build();
        return workflowClient.newWorkflowStub(SendPaymentWorkflow.class, options);
    }
}
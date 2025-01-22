package com.payments.frontdoor.config;

import com.payments.frontdoor.workflows.HighPriorityWorkflow;
import com.payments.frontdoor.workflows.PaymentWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalWorkflowConfig {

    private static final class TaskQueue {
        private static final String NORMAL = "payment_normal_subscription";
        private static final String HIGH = "payment_high_subscription";

        private TaskQueue() {} // Prevent instantiation
    }

    @Bean
    public WorkflowOptions workflowOptions() {
        return WorkflowOptions.newBuilder()
                .setTaskQueue(TaskQueue.NORMAL)
                .build();
    }

    @Bean
    public WorkflowOptions highWorkflowOptions() {
        return WorkflowOptions.newBuilder()
                .setTaskQueue(TaskQueue.HIGH)
                .build();
    }

    @Bean
    public PaymentWorkflow sendPaymentWorkflow(WorkflowClient workflowClient, WorkflowOptions workflowOptions) {
        return workflowClient.newWorkflowStub(PaymentWorkflow.class, workflowOptions);
    }

    @Bean
    public HighPriorityWorkflow highPaymentWorkflow(WorkflowClient workflowClient, WorkflowOptions highWorkflowOptions) {
        return workflowClient.newWorkflowStub(HighPriorityWorkflow.class, highWorkflowOptions);
    }

    public PaymentWorkflow sendPaymentWorkflowWithId(WorkflowClient workflowClient, String workflowId) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(TaskQueue.NORMAL)
                .setWorkflowId(workflowId)
                .build();
        return workflowClient.newWorkflowStub(PaymentWorkflow.class, options);
    }

    public HighPriorityWorkflow highPaymentWorkflowWithId(WorkflowClient workflowClient, String workflowId) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(TaskQueue.HIGH)
                .setWorkflowId(workflowId)
                .build();
        return workflowClient.newWorkflowStub(HighPriorityWorkflow.class, options);
    }
}
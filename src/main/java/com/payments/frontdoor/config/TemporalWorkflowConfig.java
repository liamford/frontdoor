package com.payments.frontdoor.config;

import com.payments.frontdoor.workflows.CrossBoarderPaymentWorkflow;
import com.payments.frontdoor.workflows.HighPriorityWorkflow;
import com.payments.frontdoor.workflows.PaymentWorkflow;
import com.payments.frontdoor.workflows.ProcessScheduler;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalWorkflowConfig {

    private static final class TaskQueue {
        private static final String NORMAL = "payment_normal_subscription";
        private static final String HIGH = "payment_high_subscription";
        private static final String CROSS_BOARDER = "payment_cb_subscription";

        private TaskQueue() {} // Prevent instantiation
    }

    @Value("${payments.scheduler.batch-payment}")
    private String batchPaymentCron;

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
    public WorkflowOptions cbworkflowOptions() {
        return WorkflowOptions.newBuilder()
                .setTaskQueue(TaskQueue.CROSS_BOARDER)
                .build();
    }

    @Bean
    public WorkflowOptions cronWorkflowOptions() {
        return WorkflowOptions.newBuilder()
            .setTaskQueue(TaskQueue.NORMAL)
            .setCronSchedule(batchPaymentCron)
            .build();
    }

    @Bean
    public PaymentWorkflow sendPaymentWorkflow(WorkflowClient workflowClient, WorkflowOptions workflowOptions) {
        return workflowClient.newWorkflowStub(PaymentWorkflow.class, workflowOptions);
    }

    @Bean
    public ProcessScheduler processScheduler(WorkflowClient workflowClient, WorkflowOptions cronWorkflowOptions) {
        return workflowClient.newWorkflowStub(ProcessScheduler.class, cronWorkflowOptions);
    }

    @Bean
    public HighPriorityWorkflow highPaymentWorkflow(WorkflowClient workflowClient, WorkflowOptions highWorkflowOptions) {
        return workflowClient.newWorkflowStub(HighPriorityWorkflow.class, highWorkflowOptions);
    }

    @Bean
    public CrossBoarderPaymentWorkflow crossBoarderPaymentWorkflow(WorkflowClient workflowClient, WorkflowOptions cbworkflowOptions) {
        return workflowClient.newWorkflowStub(CrossBoarderPaymentWorkflow.class, cbworkflowOptions);
    }

    public PaymentWorkflow sendPaymentWorkflowWithId(WorkflowClient workflowClient, String workflowId) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(TaskQueue.NORMAL)
                .setWorkflowId(workflowId)
                .build();
        return workflowClient.newWorkflowStub(PaymentWorkflow.class, options);
    }

    public CrossBoarderPaymentWorkflow sendCrossBoarderPaymentWorkflowWithId(WorkflowClient workflowClient, String workflowId) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(TaskQueue.CROSS_BOARDER)
                .setWorkflowId(workflowId)
                .build();
        return workflowClient.newWorkflowStub(CrossBoarderPaymentWorkflow.class, options);
    }

    public HighPriorityWorkflow highPaymentWorkflowWithId(WorkflowClient workflowClient, String workflowId) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(TaskQueue.HIGH)
                .setWorkflowId(workflowId)
                .build();
        return workflowClient.newWorkflowStub(HighPriorityWorkflow.class, options);
    }

    public ProcessScheduler processSchedulerWithId(WorkflowClient workflowClient, String workflowId) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
            .setTaskQueue(TaskQueue.NORMAL)
            .setWorkflowId(workflowId)
            .setCronSchedule(batchPaymentCron)
            .build();
        return workflowClient.newWorkflowStub(ProcessScheduler.class, options);
    }

}

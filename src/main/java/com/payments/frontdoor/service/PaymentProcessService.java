package com.payments.frontdoor.service;

import com.google.protobuf.Timestamp;
import com.payments.frontdoor.activities.PaymentStepStatus;
import com.payments.frontdoor.config.TemporalWorkflowConfig;
import com.payments.frontdoor.model.ActivityResult;
import com.payments.frontdoor.model.CrossBoarderPaymentDetails;
import com.payments.frontdoor.model.PaymentDetails;
import com.payments.frontdoor.model.WorkflowResult;
import com.payments.frontdoor.workflows.CrossBoarderPaymentWorkflow;
import com.payments.frontdoor.workflows.HighPriorityWorkflow;
import com.payments.frontdoor.workflows.PaymentWorkflow;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.EventType;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.history.v1.ActivityTaskCompletedEventAttributes;
import io.temporal.api.history.v1.ActivityTaskFailedEventAttributes;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryRequest;
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryResponse;
import io.temporal.client.ActivityCompletionClient;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentProcessService {

    private final WorkflowClient workflowClient;
    private final TemporalWorkflowConfig temporalWorkflowConfig;
    private final WorkflowServiceStubs service;


    @Async
    public void processPaymentAsync(PaymentDetails paymentDetails) {
        switch (paymentDetails.getPriority()) {
            case HIGH:
                HighPriorityWorkflow highWorkflow = temporalWorkflowConfig.highPaymentWorkflowWithId(
                        workflowClient,
                        paymentDetails.getPaymentId()
                );
                WorkflowClient.start(highWorkflow::processPayment, paymentDetails);
                break;

            case NORMAL:
            default:
                PaymentWorkflow normalWorkflow = temporalWorkflowConfig.sendPaymentWorkflowWithId(
                        workflowClient,
                        paymentDetails.getPaymentId()
                );
                WorkflowClient.start(normalWorkflow::processPayment, paymentDetails);
                break;
        }
    }

    @Async
    public void processCrossBoarderPaymentAsync(CrossBoarderPaymentDetails paymentDetails) {
        log.info("Processing cross boarder payment with id: {}", paymentDetails.getPaymentId());
        CrossBoarderPaymentWorkflow crossBoarderPaymentWorkflow = temporalWorkflowConfig.sendCrossBoarderPaymentWorkflowWithId(
                workflowClient,
                paymentDetails.getPaymentId()
        );
        WorkflowClient.start(crossBoarderPaymentWorkflow::processPayment, paymentDetails);
    }

    public void sendSignal(PaymentStepStatus status, String workflowId){
        PaymentWorkflow workflow = workflowClient.newWorkflowStub(PaymentWorkflow.class, workflowId);
        workflow.waitForStep(status);
    }

    public WorkflowExecutionStatus getWorkflowStatus(String workflowId) {
        DescribeWorkflowExecutionRequest describeRequest = DescribeWorkflowExecutionRequest.newBuilder()
                .setNamespace(workflowClient.getOptions().getNamespace())
                .setExecution(WorkflowExecution.newBuilder().setWorkflowId(workflowId).build())
                .build();

        DescribeWorkflowExecutionResponse describeResponse = service.blockingStub().describeWorkflowExecution(describeRequest);
        return describeResponse.getWorkflowExecutionInfo().getStatus();
    }

    public WorkflowResult retrieveWorkFlowHistory(String workflowId, boolean includeActivities) {
        List<ActivityResult> activities = null;

        DescribeWorkflowExecutionRequest describeRequest = DescribeWorkflowExecutionRequest.newBuilder()
                .setNamespace(workflowClient.getOptions().getNamespace())
                .setExecution(WorkflowExecution.newBuilder().setWorkflowId(workflowId).build())
                .build();

        DescribeWorkflowExecutionResponse describeResponse = service.blockingStub().describeWorkflowExecution(describeRequest);
        WorkflowExecutionStatus workflowStatus = describeResponse.getWorkflowExecutionInfo().getStatus();
        Timestamp workflowStartTime = describeResponse.getWorkflowExecutionInfo().getStartTime();
        Timestamp workflowEndTime = describeResponse.getWorkflowExecutionInfo().getCloseTime();
        String workflowType = describeResponse.getWorkflowExecutionInfo().getType().getName();

        if (includeActivities) {
            GetWorkflowExecutionHistoryRequest request = GetWorkflowExecutionHistoryRequest.newBuilder()
                    .setNamespace(workflowClient.getOptions().getNamespace())
                    .setExecution(WorkflowExecution.newBuilder().setWorkflowId(workflowId).build())
                    .build();

            GetWorkflowExecutionHistoryResponse response = service.blockingStub().getWorkflowExecutionHistory(request);
            Map<Long, HistoryEvent> eventsById = response.getHistory().getEventsList().stream()
                    .collect(Collectors.toMap(HistoryEvent::getEventId, event -> event));

            activities = response.getHistory().getEventsList().stream()
                    .filter(event -> event.getEventType() == EventType.EVENT_TYPE_ACTIVITY_TASK_COMPLETED || event.getEventType() == EventType.EVENT_TYPE_ACTIVITY_TASK_FAILED)
                    .map(event -> {
                        String status;
                        ActivityTaskCompletedEventAttributes completedTaskAttributes = null;
                        ActivityTaskFailedEventAttributes failedTaskAttributes = null;

                        if (event.getEventType() == EventType.EVENT_TYPE_ACTIVITY_TASK_COMPLETED) {
                            completedTaskAttributes = event.getActivityTaskCompletedEventAttributes();
                            status = "successful";
                        } else {
                            failedTaskAttributes = event.getActivityTaskFailedEventAttributes();
                            status = "failed";
                        }

                        return mapToActivityResult(completedTaskAttributes, failedTaskAttributes, eventsById, status);
                    })
                    .toList();
        }

        return new WorkflowResult(workflowStatus, workflowStartTime, workflowEndTime, workflowType, activities);
    }

    private ActivityResult mapToActivityResult(ActivityTaskCompletedEventAttributes completedTaskAttributes,
                                               ActivityTaskFailedEventAttributes failedTaskAttributes,
                                               Map<Long, HistoryEvent> eventsById, String status) {
        long scheduledEventId;
        if (completedTaskAttributes != null) {
            scheduledEventId = completedTaskAttributes.getScheduledEventId();
        } else if (failedTaskAttributes != null) {
            scheduledEventId = failedTaskAttributes.getScheduledEventId();
        } else {
            throw new IllegalStateException("Both completed and failed task attributes are null");
        }

        if (scheduledEventId == 0) {
            return new ActivityResult("Unknown Activity", status, null);
        }

        HistoryEvent scheduledEvent = eventsById.get(scheduledEventId);
        if (scheduledEvent == null) {
            throw new IllegalStateException(
                    String.format("Scheduled event not found for ID: %d", scheduledEventId));
        }

        return new ActivityResult(
                getActivityName(scheduledEvent),
                status,
                scheduledEvent.getEventTime()
        );
    }

    private String getActivityName(HistoryEvent scheduledEvent) {
        return scheduledEvent.getActivityTaskScheduledEventAttributes()
                .getActivityType()
                .getName();
    }

    public void sendToken(byte[] token) {
        ActivityCompletionClient completionClient = workflowClient.newActivityCompletionClient();
        completionClient.complete(token, PaymentStepStatus.POSTED);

    }
}
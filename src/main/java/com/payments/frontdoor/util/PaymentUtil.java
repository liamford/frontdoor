package com.payments.frontdoor.util;

import com.payments.frontdoor.model.PaymentInstruction;
import com.payments.frontdoor.swagger.model.PaymentResponse;
import com.payments.frontdoor.workflows.RefundWorkflow;
import com.payments.frontdoor.workflows.ReportWorkflow;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;

import java.time.Instant;
import java.time.OffsetDateTime;

import static java.time.ZoneId.systemDefault;



public class PaymentUtil {

    public static PaymentResponse createPaymentResponse(String uetr, PaymentResponse.StatusEnum status) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(uetr);
        response.setStatus(status);
        return response;
    }

    public static PaymentResponse createPaymentResponse(String uetr, WorkflowExecutionStatus workflowStatus) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(uetr);

        // If workflowStatus is null, default to ACTC and return early
        if (workflowStatus == null) {
            response.setStatus(PaymentResponse.StatusEnum.ACTC);
            return response;
        }

        // Map the workflow status to appropriate payment status
        PaymentResponse.StatusEnum status = switch (workflowStatus) {
            case WORKFLOW_EXECUTION_STATUS_COMPLETED -> PaymentResponse.StatusEnum.ACSC;
            case WORKFLOW_EXECUTION_STATUS_FAILED,
                 WORKFLOW_EXECUTION_STATUS_CANCELED,
                 WORKFLOW_EXECUTION_STATUS_TERMINATED,
                 WORKFLOW_EXECUTION_STATUS_TIMED_OUT -> PaymentResponse.StatusEnum.RJCT;
            case WORKFLOW_EXECUTION_STATUS_CONTINUED_AS_NEW,
                 WORKFLOW_EXECUTION_STATUS_RUNNING -> PaymentResponse.StatusEnum.ACTC;
            default -> PaymentResponse.StatusEnum.ACTC;
        };

        response.setStatus(status);
        return response;
    }

    public static OffsetDateTime convertToOffsetDateTime(com.google.protobuf.Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos())
                .atOffset(systemDefault().getRules().getOffset(Instant.now()));
    }

    public static void startRefundWorkflow(PaymentInstruction instruction) {
        ChildWorkflowOptions childWorkflowOptions = ChildWorkflowOptions.newBuilder()
                .setWorkflowId(instruction.getPaymentId() + "-refund")
                .build();
        RefundWorkflow refundWorkflow = Workflow.newChildWorkflowStub(RefundWorkflow.class, childWorkflowOptions);

        refundWorkflow.processRefund(instruction);
    }

    public static void startReportWorkflow(PaymentInstruction instruction) {
        ChildWorkflowOptions childWorkflowOptions = ChildWorkflowOptions.newBuilder()
                .setWorkflowId(instruction.getPaymentId() + "-report")
                .build();
        ReportWorkflow reportWorkflow = Workflow.newChildWorkflowStub(ReportWorkflow.class, childWorkflowOptions);
        reportWorkflow.processReporting(instruction);
    }


}

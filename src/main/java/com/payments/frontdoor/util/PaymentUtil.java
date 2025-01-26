package com.payments.frontdoor.util;

import com.payments.frontdoor.model.*;
import com.payments.frontdoor.swagger.model.*;
import com.payments.frontdoor.workflows.RefundWorkflow;
import com.payments.frontdoor.workflows.ReportWorkflow;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.time.ZoneId.systemDefault;


@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
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

    public static String generateUetr() {
        return UUID.randomUUID().toString();
    }

    public static PaymentStatusResponse convertToPaymentStatusResponse(WorkflowResult workflowResult, String paymentId) {
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setPaymentId(paymentId);
        response.setWorkflow(workflowResult.getWorkflowType());
        response.setStartTime(PaymentUtil.convertToOffsetDateTime(workflowResult.getStartTime()));
        response.setEndTime(PaymentUtil.convertToOffsetDateTime(workflowResult.getEndTime()));
        switch (workflowResult.getWorkflowStatus()) {
            case WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED:
                response.setStatus(PaymentStatusResponse.StatusEnum.ACSC);
                break;
            case WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_FAILED:
                response.setStatus(PaymentStatusResponse.StatusEnum.RJCT);
                break;
            default:
                response.setStatus(PaymentStatusResponse.StatusEnum.ACTC);
                break;
        }

        if (workflowResult.getActivities() != null) {
            response.setActivities(workflowResult.getActivities().stream()
                    .map(activity -> {
                        Activities activityResponse = new Activities();
                        activityResponse.setActivityName(activity.getActivityName());
                        activityResponse.setStatus(activity.getStatus());
                        activityResponse.setStartTime(PaymentUtil.convertToOffsetDateTime(activity.getStartTime()));
                        return activityResponse;
                    })
                    .collect(Collectors.toList()));
        }

        return response;
    }

    public static PaymentDetails getPaymentDetails(PaymentRequest request, String uetr, Map<String, String> headers) {
        return PaymentDetails.builder()
                .paymentStatus(PaymentResponse.StatusEnum.ACTC.toString())
                .paymentId(uetr)
                .debtor(request.getDebtor())
                .creditor(request.getCreditor())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentReference(request.getPaymentReference())
                .paymentDate(request.getPaymentDate())
                .priority(PaymentPriority.valueOf(Optional.ofNullable(request.getPriority())
                        .map(Enum::toString)
                        .orElse("NORMAL")))
                .headers(headers)
                .build();
    }

    public static CrossBoarderPaymentDetails getCrossBorderPaymentDetails(CrossBorderPaymentRequest request, String uetr, Map<String, String> headers) {
        return CrossBoarderPaymentDetails.builder()
                .paymentStatus(PaymentResponse.StatusEnum.ACTC.toString())
                .paymentId(uetr)
                .headers(headers)
                .customer(request.getCustomer())
                .beneficiary(request.getBeneficiary())
                .transactionDetails(request.getTransactionDetails())
                .fees(request.getFees())
                .build();
    }


    public static Object getDetails(Object request, String uetr, Map<String, String> headers) {
        return switch (request) {
            case PaymentRequest pr -> getPaymentDetails(pr, uetr, headers);
            case CrossBorderPaymentRequest cbr -> getCrossBorderPaymentDetails(cbr, uetr, headers);
            case null -> throw new IllegalArgumentException("Payment request cannot be null");
            default -> throw new IllegalArgumentException("Unsupported payment request type: " +
                    request.getClass().getSimpleName());
        };
    }

    public static String getPaymentReference(Object request) {
        return switch (request) {
            case PaymentRequest pr -> pr.getPaymentReference();
            case CrossBorderPaymentRequest cbr -> cbr.getPaymentReference();
            case null -> throw new IllegalArgumentException("Payment request cannot be null");
            default -> throw new IllegalArgumentException("Unsupported payment request type: " + request.getClass().getSimpleName());
        };
    }
}

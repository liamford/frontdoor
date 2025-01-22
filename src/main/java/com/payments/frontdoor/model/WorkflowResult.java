package com.payments.frontdoor.model;

import com.google.protobuf.Timestamp;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
public class WorkflowResult {
    private WorkflowExecutionStatus workflowStatus;
    private Timestamp startTime;
    private Timestamp endTime;
    private String workflowType;
    private List<ActivityResult> activities;
}
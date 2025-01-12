package com.payments.frontdoor.model;


import com.google.protobuf.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class ActivityResult {
    private String activityName;
    private String status;
    private Timestamp startTime;
}

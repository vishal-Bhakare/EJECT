package com.edtech.coe.batch.dto;

import com.edtech.coe.batch.entity.Enrollment;
import com.edtech.coe.batch.entity.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {

    private Long id;
    private Long batchId;
    private Long learnerId;
    private String learnerCode;
    private String learnerName;
    private String learnerEmail;
    private EnrollmentStatus status;
    private String statusReason;
    private Instant statusChangedAt;
    private Instant enrolledAt;
    private Instant updatedAt;

    // The learner's details live in the IAM module. The service looks them up and passes them
    // in, so this class does not depend on IAM. Pass null for any detail that is not known.
    public static EnrollmentResponse from(Enrollment enrollment, String learnerCode,
                                          String learnerName, String learnerEmail) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .batchId(enrollment.getBatchId())
                .learnerId(enrollment.getLearnerId())
                .learnerCode(learnerCode)
                .learnerName(learnerName)
                .learnerEmail(learnerEmail)
                .status(enrollment.getStatus())
                .statusReason(enrollment.getStatusReason())
                .statusChangedAt(enrollment.getStatusChangedAt())
                .enrolledAt(enrollment.getCreatedAt())
                .updatedAt(enrollment.getUpdatedAt())
                .build();
    }
}
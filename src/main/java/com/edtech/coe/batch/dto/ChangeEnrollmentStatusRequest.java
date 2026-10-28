package com.edtech.coe.batch.dto;

import com.edtech.coe.batch.entity.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeEnrollmentStatusRequest {

    @NotNull(message = "Status is required")
    private EnrollmentStatus status;

    // Why the learner is leaving. Used when the status is DROPPED.
    @Size(max = 255, message = "Reason must be at most 255 characters")
    private String reason;
}
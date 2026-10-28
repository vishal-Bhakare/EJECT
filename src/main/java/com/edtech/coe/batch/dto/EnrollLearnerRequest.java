package com.edtech.coe.batch.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollLearnerRequest {

    @NotNull(message = "Learner id is required")
    @Positive(message = "Learner id must be a positive number")
    private Long learnerId;
}
package com.edtech.coe.scheduling.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReassignMentorRequest {

    @NotNull(message = "New mentor id is required")
    @Positive(message = "New mentor id must be a positive number")
    private Long newMentorId;

    // Why the mentor is being replaced, for example "On sick leave"
    @Size(max = 255, message = "Reason must be at most 255 characters")
    private String reason;
}
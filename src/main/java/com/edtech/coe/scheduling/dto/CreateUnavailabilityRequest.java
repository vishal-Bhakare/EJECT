package com.edtech.coe.scheduling.dto;

import com.edtech.coe.scheduling.entity.UnavailabilityType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateUnavailabilityRequest {

    @NotNull(message = "Mentor id is required")
    @Positive(message = "Mentor id must be a positive number")
    private Long mentorId;

    @NotNull(message = "From date is required")
    private LocalDate fromDate;

    @NotNull(message = "To date is required")
    private LocalDate toDate;

    @NotNull(message = "Type is required")
    private UnavailabilityType type;

    @Size(max = 255, message = "Note must be at most 255 characters")
    private String note;
}
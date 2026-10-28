package com.edtech.coe.scheduling.dto;

import com.edtech.coe.scheduling.entity.SessionStatus;
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
public class ChangeSessionStatusRequest {

    @NotNull(message = "Status is required")
    private SessionStatus status;

    // Why the session is cancelled. Only used when the status is CANCELLED.
    @Size(max = 255, message = "Reason must be at most 255 characters")
    private String reason;
}
package com.edtech.coe.batch.dto;

import com.edtech.coe.batch.entity.BatchStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeBatchStatusRequest {

    @NotNull(message = "Status is required")
    private BatchStatus status;
}
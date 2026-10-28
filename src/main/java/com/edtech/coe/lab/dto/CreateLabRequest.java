package com.edtech.coe.lab.dto;

import com.edtech.coe.lab.entity.LabType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class CreateLabRequest {

    @NotNull(message = "COE id is required")
    @Positive(message = "COE id must be a positive number")
    private Long coeId;

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must be at most 150 characters")
    private String name;

    @NotNull(message = "Lab type is required")
    private LabType labType;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 1000, message = "Capacity must be at most 1000")
    private Integer capacity;

    @Size(max = 255, message = "Location must be at most 255 characters")
    private String location;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;
}
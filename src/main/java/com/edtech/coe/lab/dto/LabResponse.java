package com.edtech.coe.lab.dto;

import com.edtech.coe.lab.entity.Lab;
import com.edtech.coe.lab.entity.LabType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabResponse {

    private Long id;
    private String labCode;
    private Long coeId;
    private String name;
    private LabType labType;
    private Integer capacity;
    private String location;
    private String description;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static LabResponse from(Lab lab) {
        return LabResponse.builder()
                .id(lab.getId())
                .labCode(lab.getLabCode())
                .coeId(lab.getCoeId())
                .name(lab.getName())
                .labType(lab.getLabType())
                .capacity(lab.getCapacity())
                .location(lab.getLocation())
                .description(lab.getDescription())
                .active(lab.isActive())
                .createdAt(lab.getCreatedAt())
                .updatedAt(lab.getUpdatedAt())
                .build();
    }
}
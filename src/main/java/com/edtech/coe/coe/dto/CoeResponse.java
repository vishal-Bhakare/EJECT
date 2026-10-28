package com.edtech.coe.coe.dto;

import com.edtech.coe.coe.entity.Coe;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoeResponse {

    private Long id;
    private String coeCode;
    private String name;
    private String description;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;
    private String contactEmail;
    private String contactPhone;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static CoeResponse from(Coe coe) {
        return CoeResponse.builder()
                .id(coe.getId())
                .coeCode(coe.getCoeCode())
                .name(coe.getName())
                .description(coe.getDescription())
                .addressLine(coe.getAddressLine())
                .city(coe.getCity())
                .state(coe.getState())
                .pincode(coe.getPincode())
                .contactEmail(coe.getContactEmail())
                .contactPhone(coe.getContactPhone())
                .active(coe.isActive())
                .createdAt(coe.getCreatedAt())
                .updatedAt(coe.getUpdatedAt())
                .build();
    }
}
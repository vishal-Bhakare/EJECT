package com.edtech.coe.scheduling.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonSummary {

    private Long id;
    private String userCode;
    private String fullName;

    // The details live in the IAM module. The service looks them up and passes them in, so
    // this class imports nothing from there. Pass null for any detail that is not known.
    public static PersonSummary of(Long id, String userCode, String fullName) {
        return PersonSummary.builder()
                .id(id)
                .userCode(userCode)
                .fullName(fullName)
                .build();
    }
}
package com.edtech.coe.iam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorSummaryResponse {

    private Long id;
    private String userCode;
    private String fullName;

    public static MentorSummaryResponse from(UserResponse user) {
        return MentorSummaryResponse.builder()
                .id(user.getId())
                .userCode(user.getUserCode())
                .fullName(user.getFullName())
                .build();
    }
}
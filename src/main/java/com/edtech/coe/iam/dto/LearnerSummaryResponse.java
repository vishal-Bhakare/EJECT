package com.edtech.coe.iam.dto;

import com.edtech.coe.iam.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearnerSummaryResponse {

    private Long id;
    private String userCode;
    private String fullName;
    private String email;
    private boolean active;

    public static LearnerSummaryResponse from(User user) {
        return LearnerSummaryResponse.builder()
                .id(user.getId())
                .userCode(user.getUserCode())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .active(user.isActive())
                .build();
    }
}
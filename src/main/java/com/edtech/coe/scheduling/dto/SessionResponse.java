package com.edtech.coe.scheduling.dto;

import com.edtech.coe.scheduling.entity.SessionStatus;
import com.edtech.coe.scheduling.entity.TrainingSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {

    private Long id;
    private String sessionCode;
    private Long coeId;
    private Long batchId;
    private String batchName;
    private Long labId;
    private String labName;
    private Long mentorId;
    private String mentorCode;
    private String mentorName;
    private String title;
    private String description;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private SessionStatus status;
    private String statusReason;
    private Instant statusChangedAt;
    private Instant createdAt;
    private Instant updatedAt;

    // The names live in other modules (batch, lab, IAM). The service looks them up and passes
    // them in, so this class imports nothing from those modules. Pass null for any name that
    // is not known.
    public static SessionResponse from(TrainingSession session, String batchName, String labName,
                                       String mentorCode, String mentorName) {
        return SessionResponse.builder()
                .id(session.getId())
                .sessionCode(session.getSessionCode())
                .coeId(session.getCoeId())
                .batchId(session.getBatchId())
                .batchName(batchName)
                .labId(session.getLabId())
                .labName(labName)
                .mentorId(session.getMentorId())
                .mentorCode(mentorCode)
                .mentorName(mentorName)
                .title(session.getTitle())
                .description(session.getDescription())
                .sessionDate(session.getSessionDate())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .status(session.getStatus())
                .statusReason(session.getStatusReason())
                .statusChangedAt(session.getStatusChangedAt())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
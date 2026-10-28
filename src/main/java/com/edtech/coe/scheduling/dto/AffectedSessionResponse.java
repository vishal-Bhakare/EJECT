package com.edtech.coe.scheduling.dto;

import com.edtech.coe.scheduling.entity.TrainingSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AffectedSessionResponse {

    private Long sessionId;
    private String sessionCode;
    private String title;
    private Long batchId;
    private String batchName;
    private Long labId;
    private String labName;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;

    // The names live in the batch and lab modules. The service looks them up and passes them
    // in. Pass null for any name that is not known.
    public static AffectedSessionResponse from(TrainingSession session, String batchName,
                                               String labName) {
        return AffectedSessionResponse.builder()
                .sessionId(session.getId())
                .sessionCode(session.getSessionCode())
                .title(session.getTitle())
                .batchId(session.getBatchId())
                .batchName(batchName)
                .labId(session.getLabId())
                .labName(labName)
                .sessionDate(session.getSessionDate())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .build();
    }
}
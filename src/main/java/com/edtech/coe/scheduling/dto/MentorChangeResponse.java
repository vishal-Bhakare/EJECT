package com.edtech.coe.scheduling.dto;

import com.edtech.coe.scheduling.entity.SessionMentorChange;
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
public class MentorChangeResponse {

    private Long id;
    private Long sessionId;
    private String sessionCode;
    private String sessionTitle;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long coeId;
    private PersonSummary oldMentor;
    private PersonSummary newMentor;
    private String reason;
    private PersonSummary changedBy;
    private Instant changedAt;

    // The session is passed in so the response can say which class it was. The people come
    // from the IAM module, and the service looks them up. A session is never hard-deleted,
    // so it is always there.
    public static MentorChangeResponse from(SessionMentorChange change, TrainingSession session,
                                            PersonSummary oldMentor, PersonSummary newMentor,
                                            PersonSummary changedBy) {
        return MentorChangeResponse.builder()
                .id(change.getId())
                .sessionId(change.getSessionId())
                .sessionCode(session.getSessionCode())
                .sessionTitle(session.getTitle())
                .sessionDate(session.getSessionDate())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .coeId(change.getCoeId())
                .oldMentor(oldMentor)
                .newMentor(newMentor)
                .reason(change.getReason())
                .changedBy(changedBy)
                .changedAt(change.getCreatedAt())
                .build();
    }
}
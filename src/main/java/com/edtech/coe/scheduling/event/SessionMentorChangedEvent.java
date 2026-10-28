package com.edtech.coe.scheduling.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class SessionMentorChangedEvent {

    private final Long sessionId;
    private final String sessionCode;
    private final Long coeId;
    private final Long batchId;
    private final Long oldMentorId;
    private final Long newMentorId;
    private final String reason;
    private final Long changedBy;
    private final LocalDate sessionDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
}
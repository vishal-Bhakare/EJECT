package com.edtech.coe.scheduling.dto;

import com.edtech.coe.scheduling.entity.MentorUnavailability;
import com.edtech.coe.scheduling.entity.UnavailabilityType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnavailabilityResponse {

    private Long id;
    private Long coeId;
    private PersonSummary mentor;
    private LocalDate fromDate;
    private LocalDate toDate;
    private long days;
    private UnavailabilityType type;
    private String note;
    private PersonSummary recordedBy;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    // Only filled in when the record is first created: the sessions that the absence leaves
    // without a teacher. It is left out of the JSON everywhere else.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<AffectedSessionResponse> affectedSessions;

    // For lists and single records: no affected sessions
    public static UnavailabilityResponse from(MentorUnavailability record, PersonSummary mentor,
                                              PersonSummary recordedBy) {
        return from(record, mentor, recordedBy, null);
    }

    // For the response to creating a record: includes the affected sessions
    public static UnavailabilityResponse from(MentorUnavailability record, PersonSummary mentor,
                                              PersonSummary recordedBy,
                                              List<AffectedSessionResponse> affectedSessions) {
        return UnavailabilityResponse.builder()
                .id(record.getId())
                .coeId(record.getCoeId())
                .mentor(mentor)
                .fromDate(record.getFromDate())
                .toDate(record.getToDate())
                // Both dates are included, so one day is 1 and 3 to 5 February is 3
                .days(ChronoUnit.DAYS.between(record.getFromDate(), record.getToDate()) + 1)
                .type(record.getType())
                .note(record.getNote())
                .recordedBy(recordedBy)
                .active(record.isActive())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .affectedSessions(affectedSessions)
                .build();
    }
}
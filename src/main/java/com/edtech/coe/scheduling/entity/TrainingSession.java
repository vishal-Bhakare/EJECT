package com.edtech.coe.scheduling.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "training_sessions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_training_sessions_code", columnNames = "session_code")
        },
        indexes = {
                @Index(name = "idx_ts_mentor_date", columnList = "mentor_id, session_date"),
                @Index(name = "idx_ts_lab_date", columnList = "lab_id, session_date"),
                @Index(name = "idx_ts_batch_date", columnList = "batch_id, session_date"),
                @Index(name = "idx_ts_coe_date", columnList = "coe_id, session_date")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_code", nullable = false, length = 30)
    private String sessionCode;

    // Plain IDs, no JPA relations. The service checks that each one exists and belongs
    // to the same COE as the session.
    @Column(name = "coe_id", nullable = false)
    private Long coeId;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "lab_id", nullable = false)
    private Long labId;

    // The id of a user with the MENTOR role (from the IAM module)
    @Column(name = "mentor_id", nullable = false)
    private Long mentorId;

    // The topic of the class, for example "Spring Boot REST APIs"
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    // Local date and time at the COE. There is no time zone, because a 9:00 class is 9:00 on
    // the wall clock of the room it is held in.
    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.SCHEDULED;

    // Why the session was cancelled. Empty for every other status.
    @Column(name = "status_reason", length = 255)
    private String statusReason;

    // When the status last changed. Empty until it changes for the first time.
    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
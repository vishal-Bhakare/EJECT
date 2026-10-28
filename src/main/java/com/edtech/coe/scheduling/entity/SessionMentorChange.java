package com.edtech.coe.scheduling.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(
        name = "session_mentor_changes",
        indexes = {
                @Index(name = "idx_smc_session", columnList = "session_id"),
                @Index(name = "idx_smc_coe_created", columnList = "coe_id, created_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionMentorChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain IDs, no JPA relations
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    // Stored here as well, so the history can be listed for a COE without touching sessions
    @Column(name = "coe_id", nullable = false)
    private Long coeId;

    @Column(name = "old_mentor_id", nullable = false)
    private Long oldMentorId;

    @Column(name = "new_mentor_id", nullable = false)
    private Long newMentorId;

    // Why the mentor was replaced, for example "On sick leave"
    @Column(name = "reason", length = 255)
    private String reason;

    // The user who made the change
    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    // When the change was made. There is no updatedAt, because a history row never changes.
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
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

@Entity
@Table(
        name = "mentor_unavailability",
        indexes = {
                @Index(name = "idx_mu_mentor_from", columnList = "mentor_id, from_date"),
                @Index(name = "idx_mu_coe_from", columnList = "coe_id, from_date")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorUnavailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain IDs, no JPA relations. The service checks that the mentor is a mentor in this COE.
    @Column(name = "coe_id", nullable = false)
    private Long coeId;

    // The id of a user with the MENTOR role (from the IAM module)
    @Column(name = "mentor_id", nullable = false)
    private Long mentorId;

    // Whole days, both included. A single day has the same from and to date.
    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private UnavailabilityType type;

    @Column(name = "note", length = 255)
    private String note;

    // The user who recorded it (a Practice Head, COE Admin or Super Admin)
    @Column(name = "recorded_by", nullable = false)
    private Long recordedBy;

    // false means it was cancelled, for example because the mentor came back early.
    // A cancelled record is kept, but it no longer blocks anything.
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
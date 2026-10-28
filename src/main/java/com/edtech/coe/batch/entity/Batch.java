package com.edtech.coe.batch.entity;

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

@Entity
@Table(name = "batches", uniqueConstraints = {
		@UniqueConstraint(name = "uk_batches_batch_code", columnNames = "batch_code"),
		@UniqueConstraint(name = "uk_batches_coe_name", columnNames = { "coe_id", "name" }) }, indexes = {
				@Index(name = "idx_batches_coe_status", columnList = "coe_id, status") })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Batch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "batch_code", nullable = false, length = 30)
	private String batchCode;

	// Plain ID, no JPA relation. The service checks that the COE exists and is
	// active.
	@Column(name = "coe_id", nullable = false)
	private Long coeId;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	// What the batch is learning, for example "Java Full Stack"
	@Column(name = "track_name", nullable = false, length = 150)
	private String trackName;

	@Column(name = "description", length = 500)
	private String description;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	// Maximum number of learners that can be enrolled
	@Column(name = "max_learners", nullable = false)
	private Integer maxLearners;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	@Builder.Default
	private BatchStatus status = BatchStatus.PLANNED;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
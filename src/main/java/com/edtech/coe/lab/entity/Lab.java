package com.edtech.coe.lab.entity;

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

@Entity
@Table(name = "labs", uniqueConstraints = { @UniqueConstraint(name = "uk_labs_lab_code", columnNames = "lab_code"),
		@UniqueConstraint(name = "uk_labs_coe_name", columnNames = { "coe_id", "name" }) }, indexes = {
				@Index(name = "idx_labs_coe_active", columnList = "coe_id, active") })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lab {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "lab_code", nullable = false, length = 30)
	private String labCode;

	// Plain ID, no JPA relation. The service checks that the COE exists and is
	// active.
	@Column(name = "coe_id", nullable = false)
	private Long coeId;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "lab_type", nullable = false, length = 30)
	private LabType labType;

	@Column(name = "capacity", nullable = false)
	private Integer capacity;

	@Column(name = "location", length = 255)
	private String location;

	@Column(name = "description", length = 500)
	private String description;

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
package com.edtech.coe.coe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@Table(name = "coes", uniqueConstraints = { @UniqueConstraint(name = "uk_coes_coe_code", columnNames = "coe_code"),
		@UniqueConstraint(name = "uk_coes_name", columnNames = "name") }, indexes = {
				@Index(name = "idx_coes_city", columnList = "city"),
				@Index(name = "idx_coes_active", columnList = "active") })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coe {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "coe_code", nullable = false, length = 30)
	private String coeCode;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "description", length = 500)
	private String description;

	@Column(name = "address_line", length = 255)
	private String addressLine;

	@Column(name = "city", nullable = false, length = 100)
	private String city;

	@Column(name = "state", nullable = false, length = 100)
	private String state;

	@Column(name = "pincode", length = 10)
	private String pincode;

	@Column(name = "contact_email", length = 150)
	private String contactEmail;

	@Column(name = "contact_phone", length = 20)
	private String contactPhone;

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
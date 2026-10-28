package com.edtech.coe.iam.dto;

import com.edtech.coe.iam.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthenticatedUser {

	private final Long userId;
	private final String email;
	private final Role role;
	private final Long coeId; // null for SUPER_ADMIN and LEADERSHIP

	// Platform-wide roles belong to no COE and can see all of them
	public boolean isPlatformWide() {
		return role == Role.SUPER_ADMIN || role == Role.LEADERSHIP;
	}
}
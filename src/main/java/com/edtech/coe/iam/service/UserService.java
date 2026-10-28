package com.edtech.coe.iam.service;

import com.edtech.coe.iam.dto.CreateUserRequest;
import com.edtech.coe.iam.dto.UserResponse;
import com.edtech.coe.iam.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Map;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(Long id);

    Page<UserResponse> getUsers(Role role, Long coeId, Pageable pageable);

    // Looks up many users in one query. An id that does not exist is missing from the result.
    Map<Long, UserResponse> getUsersByIds(Collection<Long> ids);
}
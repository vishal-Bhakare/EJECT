package com.edtech.coe.iam.service.impl;

import com.edtech.coe.coe.service.CoeService;
import com.edtech.coe.common.exception.BusinessException;
import com.edtech.coe.common.exception.ResourceNotFoundException;
import com.edtech.coe.iam.dto.CreateUserRequest;
import com.edtech.coe.iam.dto.UserResponse;
import com.edtech.coe.iam.entity.Role;
import com.edtech.coe.iam.entity.User;
import com.edtech.coe.iam.repository.UserRepository;
import com.edtech.coe.iam.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CoeService coeService;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "A user with email " + email + " already exists");
        }

        validateCoeRule(request.getRole(), request.getCoeId());
        validateCoeIsActive(request.getCoeId());

        User user = User.builder()
                .userCode(temporaryCode())
                .fullName(request.getFullName().trim())
                .email(email)
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .coeId(request.getCoeId())
                .build();

        // First save gives us the auto-generated id
        User saved = userRepository.save(user);

        // Build the real code from the role prefix and id, e.g. MEN-0005
        saved.setUserCode(buildUserCode(saved.getRole(), saved.getId()));

        return UserResponse.from(saved);
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return UserResponse.from(user);
    }

    @Override
    public Page<UserResponse> getUsers(Role role, Long coeId, Pageable pageable) {
        Page<User> users;

        if (role != null && coeId != null) {
            users = userRepository.findByCoeIdAndRole(coeId, role, pageable);
        } else if (role != null) {
            users = userRepository.findByRole(role, pageable);
        } else if (coeId != null) {
            users = userRepository.findByCoeId(coeId, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        return users.map(UserResponse::from);
    }

    @Override
    public Map<Long, UserResponse> getUsersByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, UserResponse::from));
    }

    // ---------- private helpers ----------

    private void validateCoeRule(Role role, Long coeId) {
        boolean platformWide = role == Role.SUPER_ADMIN || role == Role.LEADERSHIP;

        if (platformWide && coeId != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    role + " is platform-wide and must not have a coeId");
        }
        if (!platformWide && coeId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "coeId is required for role " + role);
        }
    }

    private void validateCoeIsActive(Long coeId) {
        // Platform-wide roles have no coeId, so there is nothing to check
        if (coeId != null && !coeService.isActiveCoe(coeId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "COE with id " + coeId + " does not exist or is inactive");
        }
    }

    private String temporaryCode() {
        // max 30 chars: "TMP-" (4) + 20 = 24
        return "TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private String buildUserCode(Role role, Long id) {
        return rolePrefix(role) + "-" + String.format("%04d", id);
    }

    private String rolePrefix(Role role) {
        return switch (role) {
            case SUPER_ADMIN -> "SUP";
            case LEADERSHIP -> "LDR";
            case COE_ADMIN -> "CAD";
            case PRACTICE_HEAD -> "PHD";
            case HR -> "HR";
            case MENTOR -> "MEN";
            case LEARNER -> "LRN";
        };
    }
}
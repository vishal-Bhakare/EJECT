package com.edtech.coe.iam.service.impl;

import com.edtech.coe.common.exception.BusinessException;
import com.edtech.coe.iam.dto.LoginRequest;
import com.edtech.coe.iam.dto.LoginResponse;
import com.edtech.coe.iam.dto.UserResponse;
import com.edtech.coe.iam.entity.User;
import com.edtech.coe.iam.repository.UserRepository;
import com.edtech.coe.iam.service.AuthService;
import com.edtech.coe.iam.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }

        // Checked after the password, so only the real owner learns the account is disabled
        if (!user.isActive()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        user.setLastLoginAt(Instant.now());   // saved automatically at commit

        return LoginResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .tokenType("Bearer")
                .expiresInSeconds(jwtService.getExpirationSeconds())
                .user(UserResponse.from(user))
                .build();
    }
}
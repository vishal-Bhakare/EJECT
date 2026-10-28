package com.edtech.coe.iam.bootstrap;

import com.edtech.coe.iam.dto.CreateUserRequest;
import com.edtech.coe.iam.entity.Role;
import com.edtech.coe.iam.repository.UserRepository;
import com.edtech.coe.iam.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${app.bootstrap.admin-name}")
    private String adminName;

    @Value("${app.bootstrap.admin-email}")
    private String adminEmail;

    @Value("${app.bootstrap.admin-password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.SUPER_ADMIN)) {
            log.info("SUPER_ADMIN already exists, bootstrap skipped");
            return;
        }

        CreateUserRequest request = new CreateUserRequest();
        request.setFullName(adminName);
        request.setEmail(adminEmail);
        request.setPassword(adminPassword);
        request.setRole(Role.SUPER_ADMIN);   // coeId stays null

        userService.createUser(request);
        log.warn("Bootstrap SUPER_ADMIN created with email {}", adminEmail);
    }
}
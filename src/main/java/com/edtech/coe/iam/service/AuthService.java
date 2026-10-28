package com.edtech.coe.iam.service;

import com.edtech.coe.iam.dto.LoginRequest;
import com.edtech.coe.iam.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
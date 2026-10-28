package com.edtech.coe.iam.service;

import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.iam.entity.User;

public interface JwtService {

    String generateToken(User user);

    long getExpirationSeconds();

    // Throws io.jsonwebtoken.JwtException if the token is invalid, tampered or expired
    AuthenticatedUser parseToken(String token);
}
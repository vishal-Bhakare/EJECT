package com.edtech.coe.iam.service.impl;

import com.edtech.coe.iam.entity.User;
import com.edtech.coe.iam.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.iam.entity.Role;
import io.jsonwebtoken.Claims;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-minutes}")
    private long expirationMinutes;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        // Fails fast at startup if the secret is too short
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(getExpirationSeconds());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .claim("coeId", user.getCoeId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }
    
    @Override
    public AuthenticatedUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)      // checks the signature
                .build()
                .parseSignedClaims(token)    // also checks the expiry
                .getPayload();

        // Numbers may be read back as Integer, so read as Number and convert
        Number userId = claims.get("userId", Number.class);
        Number coeId = claims.get("coeId", Number.class);   // null when absent

        return new AuthenticatedUser(
                userId.longValue(),
                claims.getSubject(),
                Role.valueOf(claims.get("role", String.class)),
                coeId == null ? null : coeId.longValue()
        );
    }
}
package com.copap.auth;

import java.time.Instant;

public class AuthToken {

    private final String token;
    private final String userId;
    private final Instant expiresAt;

    public AuthToken(String token, String userId, Instant expiresAt) {
        this.token = token;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }
    public String getToken() {
        return token;
    }
    public String getUserId() {
        return userId;
    }
    public Instant getExpiresAt() {
        return expiresAt;
    }
}
package com.copap.auth;

import java.util.Optional;

public interface AuthTokenRepository {
    void save(AuthToken authToken);
    Optional<AuthToken> findByToken(String token);
}
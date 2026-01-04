package com.copap.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class AuthService {

    private final UserRepository userRepository;
    private final AuthTokenRepository tokenRepository;

    public AuthService(UserRepository userRepository,
                       AuthTokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    public String login(String username, String password) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!PasswordHasher.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        String tokenValue = UUID.randomUUID().toString();

        AuthToken token = new AuthToken(
                tokenValue,
                user.getUserId(),
                Instant.now().plus(1, ChronoUnit.HOURS)
        );

        tokenRepository.save(token);

        return tokenValue;
    }
}
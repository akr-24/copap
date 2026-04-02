package com.copap.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuthTokenRepository tokenRepository;
    private final BCryptPasswordEncoder bcryptEncoder;

    public AuthService(UserRepository userRepository,
                       AuthTokenRepository tokenRepository,
                       BCryptPasswordEncoder bcryptEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.bcryptEncoder = bcryptEncoder;
    }

    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        boolean valid;
        if ("bcrypt".equalsIgnoreCase(user.getPasswordAlgo())) {
            valid = bcryptEncoder.matches(password, user.getPasswordHash());
        } else {
            // Legacy SHA-256 path — verify then upgrade to BCrypt on success
            valid = PasswordHasher.matches(password, user.getPasswordHash());
            if (valid) {
                String newHash = bcryptEncoder.encode(password);
                ((JdbcUserRepository) userRepository).upgradePassword(user.getUserId(), newHash);
            }
        }

        if (!valid) {
            throw new RuntimeException("Invalid credentials");
        }

        return issueToken(user.getUserId());
    }

    public String register(String username, String password, String email) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        String userId = UUID.randomUUID().toString();
        String passwordHash = bcryptEncoder.encode(password);
        userRepository.save(userId, username, passwordHash, email, "USER", "bcrypt");

        return issueToken(userId);
    }

    private String issueToken(String userId) {
        String tokenValue = UUID.randomUUID().toString();
        AuthToken token = new AuthToken(
                tokenValue,
                userId,
                Instant.now().plus(1, ChronoUnit.HOURS)
        );
        tokenRepository.save(token);
        return tokenValue;
    }
}

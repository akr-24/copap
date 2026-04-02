package com.copap.auth;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    Optional<User> findById(String userId);
    void save(String userId, String username, String passwordHash, String email, String role, String passwordAlgo);
}

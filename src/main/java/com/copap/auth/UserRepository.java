package com.copap.auth;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);
}
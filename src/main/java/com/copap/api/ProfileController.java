package com.copap.api;

import com.copap.auth.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ProfileController {

    private final UserRepository userRepository;

    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, String>> getProfile(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return userRepository.findById(userId)
                .map(u -> ResponseEntity.ok(Map.of(
                        "userId", u.getUserId(),
                        "username", u.getUsername(),
                        "email", u.getEmail() != null ? u.getEmail() : "",
                        "role", u.getRole()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}

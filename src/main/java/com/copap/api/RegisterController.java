package com.copap.api;

import com.copap.auth.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RegisterController {

    private final AuthService authService;

    public RegisterController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        String token = authService.register(request.username(), request.password(), request.email());
        return ResponseEntity.ok(Map.of("token", token));
    }

    public record RegisterRequest(
            @NotBlank String username,
            @NotBlank String password,
            String email
    ) {}
}

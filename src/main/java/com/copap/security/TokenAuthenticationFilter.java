package com.copap.security;

import com.copap.auth.AuthTokenRepository;
import com.copap.auth.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final AuthTokenRepository authTokenRepository;
    private final UserRepository userRepository;

    public TokenAuthenticationFilter(AuthTokenRepository authTokenRepository,
                                     UserRepository userRepository) {
        this.authTokenRepository = authTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String rawToken = authHeader.substring(7);

            authTokenRepository.findByToken(rawToken).ifPresent(authToken -> {
                if (authToken.getExpiresAt().isAfter(Instant.now())) {
                    userRepository.findById(authToken.getUserId()).ifPresent(user -> {
                        var authorities = List.of(
                                new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase())
                        );
                        var authentication = new UsernamePasswordAuthenticationToken(
                                user.getUserId(), null, authorities
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
                }
            });
        }

        filterChain.doFilter(request, response);
    }
}

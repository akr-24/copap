package com.copap.auth;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.time.Instant;

public class AuthFilter extends Filter {

    private final AuthTokenRepository tokenRepository;

    public AuthFilter(AuthTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain)
            throws IOException {

        System.out.println("authfilter being hit");
        String authHeader =
                exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
            return;
        }

        String tokenValue = authHeader.substring("Bearer ".length());

        AuthToken token = tokenRepository
                .findByToken(tokenValue)
                .orElse(null);

        if (token == null || token.getExpiresAt().isBefore(Instant.now())) {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
            return;
        }

        // Token valid → continue
        chain.doFilter(exchange);
    }

    @Override
    public String description() {
        return "Auth token filter";
    }
}
package com.copap.api.exception;

import java.time.Instant;

public record ErrorResponse(String error, int status, Instant timestamp) {
    public ErrorResponse(String error, int status) {
        this(error, status, Instant.now());
    }
}

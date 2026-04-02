package com.copap.api.exception;

public class InvalidOrderTransitionException extends RuntimeException {
    public InvalidOrderTransitionException(String message) {
        super(message);
    }
}

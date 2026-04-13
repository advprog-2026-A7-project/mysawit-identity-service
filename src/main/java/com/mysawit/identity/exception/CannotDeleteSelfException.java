package com.mysawit.identity.exception;

public class CannotDeleteSelfException extends RuntimeException {
    public CannotDeleteSelfException(String message) {
        super(message);
    }
}

package com.mysawit.identity.exception;

public class MissingGoogleRegistrationFieldException extends RuntimeException {
    public MissingGoogleRegistrationFieldException(String message) {
        super(message);
    }
}

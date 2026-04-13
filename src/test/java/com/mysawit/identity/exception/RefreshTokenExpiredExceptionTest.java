package com.mysawit.identity.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenExpiredExceptionTest {

    @Test
    void constructorSetsMessage() {
        RefreshTokenExpiredException ex = new RefreshTokenExpiredException("expired");
        assertEquals("expired", ex.getMessage());
    }
}

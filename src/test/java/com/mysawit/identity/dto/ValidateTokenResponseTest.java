package com.mysawit.identity.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidateTokenResponseTest {

    @Test
    void noArgConstructorCreatesInstance() {
        ValidateTokenResponse response = new ValidateTokenResponse();

        assertFalse(response.isValid());
        assertNull(response.getUsername());
    }

    @Test
    void settersUpdateFields() {
        ValidateTokenResponse response = new ValidateTokenResponse();
        response.setValid(true);
        response.setUsername("user@mail.com");

        assertTrue(response.isValid());
        assertEquals("user@mail.com", response.getUsername());
    }
}

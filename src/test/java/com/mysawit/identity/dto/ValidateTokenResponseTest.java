package com.mysawit.identity.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ValidateTokenResponseTest {

    @Test
    void noArgsConstructorAndSettersWork() {
        ValidateTokenResponse response = new ValidateTokenResponse();

        assertFalse(response.isValid());
        assertNull(response.getUsername());

        response.setValid(true);
        response.setUsername("user@mail.com");

        assertTrue(response.isValid());
        assertEquals("user@mail.com", response.getUsername());
    }

    @Test
    void allArgsConstructorSetsFields() {
        ValidateTokenResponse response = new ValidateTokenResponse(true, "admin@mail.com");

        assertTrue(response.isValid());
        assertEquals("admin@mail.com", response.getUsername());
    }
}

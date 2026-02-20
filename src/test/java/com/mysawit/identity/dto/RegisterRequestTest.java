package com.mysawit.identity.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestTest {

    @Test
    void gettersAndSettersWork() {
        RegisterRequest request = new RegisterRequest();

        request.setUsername("user");
        request.setEmail("user@mail.com");
        request.setPassword("secret123");

        assertEquals("user", request.getUsername());
        assertEquals("user@mail.com", request.getEmail());
        assertEquals("secret123", request.getPassword());
    }
}

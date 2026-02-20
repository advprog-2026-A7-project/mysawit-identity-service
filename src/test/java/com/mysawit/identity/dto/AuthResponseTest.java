package com.mysawit.identity.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthResponseTest {

    @Test
    void constructorAndSettersWork() {
        AuthResponse response = new AuthResponse("token", 1L, "user", "user@mail.com", "USER");

        assertEquals("token", response.getToken());
        assertEquals("Bearer", response.getType());
        assertEquals(1L, response.getId());
        assertEquals("user", response.getUsername());
        assertEquals("user@mail.com", response.getEmail());
        assertEquals("USER", response.getRole());

        response.setToken("new-token");
        response.setType("Custom");
        response.setId(2L);
        response.setUsername("new-user");
        response.setEmail("new@mail.com");
        response.setRole("ADMIN");

        assertEquals("new-token", response.getToken());
        assertEquals("Custom", response.getType());
        assertEquals(2L, response.getId());
        assertEquals("new-user", response.getUsername());
        assertEquals("new@mail.com", response.getEmail());
        assertEquals("ADMIN", response.getRole());
    }
}

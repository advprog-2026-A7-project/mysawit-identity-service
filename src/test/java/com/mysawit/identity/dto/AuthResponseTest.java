package com.mysawit.identity.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthResponseTest {

    @Test
    void constructorAndSettersWork() {
        AuthResponse response = new AuthResponse("token", "refresh", "1", "user", "user@mail.com", "USER");

        assertEquals("token", response.getToken());
        assertEquals("refresh", response.getRefreshToken());
        assertEquals("Bearer", response.getType());
        assertEquals("1", response.getId());
        assertEquals("user", response.getUsername());
        assertEquals("user@mail.com", response.getEmail());
        assertEquals("USER", response.getRole());

        response.setToken("new-token");
        response.setRefreshToken("new-refresh");
        response.setType("Custom");
        response.setId("2");
        response.setUsername("new-user");
        response.setEmail("new@mail.com");
        response.setRole("ADMIN");

        assertEquals("new-token", response.getToken());
        assertEquals("new-refresh", response.getRefreshToken());
        assertEquals("Custom", response.getType());
        assertEquals("2", response.getId());
        assertEquals("new-user", response.getUsername());
        assertEquals("new@mail.com", response.getEmail());
        assertEquals("ADMIN", response.getRole());
    }
}

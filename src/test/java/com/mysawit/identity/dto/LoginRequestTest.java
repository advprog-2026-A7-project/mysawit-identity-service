package com.mysawit.identity.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRequestTest {

    @Test
    void gettersAndSettersWork() {
        LoginRequest request = new LoginRequest();

        request.setUsername("user");
        request.setPassword("secret");

        assertEquals("user", request.getUsername());
        assertEquals("secret", request.getPassword());
    }
}

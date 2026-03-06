package com.mysawit.identity.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRequestTest {

    @Test
    void gettersAndSettersWork() {
        LoginRequest request = new LoginRequest();

        request.setEmail("user@mail.com");
        request.setPassword("secret");

        assertEquals("user@mail.com", request.getEmail());
        assertEquals("secret", request.getPassword());
    }

    @Test
    void usernameAliasMapsToEmail() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        LoginRequest request = objectMapper.readValue(
                "{\"username\":\"alias@mail.com\",\"password\":\"secret\"}",
                LoginRequest.class
        );

        assertEquals("alias@mail.com", request.getEmail());
        assertEquals("secret", request.getPassword());
    }
}

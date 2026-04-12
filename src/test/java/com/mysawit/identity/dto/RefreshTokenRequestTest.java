package com.mysawit.identity.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenRequestTest {

    @Test
    void noArgsConstructorAndSettersWork() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("token-value");

        assertEquals("token-value", request.getRefreshToken());
    }

    @Test
    void allArgsConstructorWorks() {
        RefreshTokenRequest request = new RefreshTokenRequest("token-value");

        assertEquals("token-value", request.getRefreshToken());
    }
}

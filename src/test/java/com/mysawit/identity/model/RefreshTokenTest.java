package com.mysawit.identity.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenTest {

    @Test
    void builderWorks() {
        LocalDateTime expires = LocalDateTime.now().plusDays(7);
        RefreshToken token = RefreshToken.builder()
                .id("id-1")
                .token("token-value")
                .userId("user-1")
                .expiresAt(expires)
                .createdAt(LocalDateTime.now())
                .build();

        assertEquals("id-1", token.getId());
        assertEquals("token-value", token.getToken());
        assertEquals("user-1", token.getUserId());
        assertEquals(expires, token.getExpiresAt());
        assertNotNull(token.getCreatedAt());
    }

    @Test
    void noArgsConstructorAndSettersWork() {
        RefreshToken token = new RefreshToken();
        token.setId("id-1");
        token.setToken("token-value");
        token.setUserId("user-1");

        assertEquals("id-1", token.getId());
        assertEquals("token-value", token.getToken());
        assertEquals("user-1", token.getUserId());
    }

    @Test
    void onCreateSetsIdAndCreatedAt() {
        RefreshToken token = new RefreshToken();
        token.setToken("value");
        token.setUserId("user-1");
        token.setExpiresAt(LocalDateTime.now().plusDays(7));

        token.onCreate();

        assertNotNull(token.getId());
        assertNotNull(token.getCreatedAt());
    }

    @Test
    void onCreateDoesNotOverrideExistingId() {
        RefreshToken token = new RefreshToken();
        token.setId("existing-id");
        token.setCreatedAt(LocalDateTime.of(2020, 1, 1, 0, 0));

        token.onCreate();

        assertEquals("existing-id", token.getId());
        assertEquals(LocalDateTime.of(2020, 1, 1, 0, 0), token.getCreatedAt());
    }

    @Test
    void allArgsConstructorWorks() {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken token = new RefreshToken("id", "tok", "uid", now, now);

        assertEquals("id", token.getId());
        assertEquals("tok", token.getToken());
        assertEquals("uid", token.getUserId());
    }
}

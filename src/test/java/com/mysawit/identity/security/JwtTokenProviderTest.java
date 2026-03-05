package com.mysawit.identity.security;

import com.mysawit.identity.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "mysawit-secret-key-change-in-production-2026-very-long-for-tests");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", 3600000L);
    }

    @Test
    void generateAndParseTokenWorks() {
        String token = jwtTokenProvider.generateToken("user", "1", Role.BURUH);

        assertNotNull(token);
        assertEquals("user", jwtTokenProvider.getUsernameFromToken(token));
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void validateTokenReturnsFalseForInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid-token"));
    }
}

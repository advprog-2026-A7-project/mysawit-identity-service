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
        ReflectionTestUtils.setField(jwtTokenProvider, "accessExpiration", 3600000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpiration", 604800000L);
    }

    @Test
    void generateAndParseTokenWorks() {
        String token = jwtTokenProvider.generateToken("1", Role.BURUH);

        assertNotNull(token);
        assertEquals("1", jwtTokenProvider.getUsernameFromToken(token));
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void parseInternalTokenAndVerifyClaims() {
        String token = jwtTokenProvider.generateToken("1", Role.BURUH);

        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) ReflectionTestUtils.invokeMethod(jwtTokenProvider, "getSigningKey"))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("1", claims.getSubject());
        assertEquals("1", claims.get("userId"));
        assertEquals("BURUH", claims.get("role"));
    }

    @Test
    void validateTokenReturnsFalseForInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid-token"));
    }

    @Test
    void generateRefreshTokenValueReturnsNonNull() {
        String refreshToken = jwtTokenProvider.generateRefreshTokenValue();

        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
    }

    @Test
    void generateRefreshTokenValueReturnsUniqueValues() {
        String token1 = jwtTokenProvider.generateRefreshTokenValue();
        String token2 = jwtTokenProvider.generateRefreshTokenValue();

        assertNotEquals(token1, token2);
    }

    @Test
    void getRefreshExpirationReturnsConfiguredValue() {
        assertEquals(604800000L, jwtTokenProvider.getRefreshExpiration());
    }

    @Test
    void getAuthenticationReturnsCorrectPrincipal() {
        String token = jwtTokenProvider.generateToken("user-1", Role.ADMIN);

        org.springframework.security.core.Authentication auth = jwtTokenProvider.getAuthentication(token);

        assertEquals("user-1", auth.getName());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }
}

package com.mysawit.identity.service;

import com.mysawit.identity.dto.AuthResponse;
import com.mysawit.identity.dto.ValidateTokenResponse;
import com.mysawit.identity.enums.Role;
import com.mysawit.identity.exception.InvalidTokenException;
import com.mysawit.identity.exception.RefreshTokenExpiredException;
import com.mysawit.identity.model.RefreshToken;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.RefreshTokenRepository;
import com.mysawit.identity.repository.UserRepository;
import com.mysawit.identity.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TokenServiceTest {

    private JwtTokenProvider jwtTokenProvider;
    private RefreshTokenRepository refreshTokenRepository;
    private UserRepository userRepository;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        userRepository = mock(UserRepository.class);
        tokenService = new TokenService(jwtTokenProvider, refreshTokenRepository, userRepository);

        when(jwtTokenProvider.generateRefreshTokenValue()).thenReturn("refresh-token");
        when(jwtTokenProvider.getRefreshExpiration()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void issueTokensSavesRefreshTokenAndBuildsResponse() {
        User user = new User();
        user.setId("u1");
        user.setName("User");
        user.setEmail("user@mail.com");
        user.setPassword("encoded");
        user.setRole(Role.BURUH);

        when(jwtTokenProvider.generateToken("u1", Role.BURUH)).thenReturn("jwt-1");

        AuthResponse response = tokenService.issueTokens(user);

        assertEquals("jwt-1", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("u1", response.getId());
        assertEquals("User", response.getUsername());
        assertEquals("user@mail.com", response.getEmail());
        assertEquals("BURUH", response.getRole());
        assertFalse(response.getGoogleLinked());
        assertTrue(response.getHasPassword());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken stored = captor.getValue();
        assertEquals("refresh-token", stored.getToken());
        assertEquals("u1", stored.getUserId());
        assertNotNull(stored.getExpiresAt());
        assertTrue(stored.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void issueTokensReflectsGoogleLinkedAndNoPassword() {
        User user = new User();
        user.setId("u2");
        user.setName("Google User");
        user.setEmail("g@mail.com");
        user.setRole(Role.SUPIR);
        user.setGoogleSub("google-sub-x");
        user.setPassword(null);

        when(jwtTokenProvider.generateToken("u2", Role.SUPIR)).thenReturn("jwt-2");

        AuthResponse response = tokenService.issueTokens(user);

        assertTrue(response.getGoogleLinked());
        assertFalse(response.getHasPassword());
    }

    @Test
    void validateReturnsResponseWhenTokenValid() {
        when(jwtTokenProvider.validateToken("v")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("v")).thenReturn("user@mail.com");

        ValidateTokenResponse response = tokenService.validate("v");

        assertTrue(response.isValid());
        assertEquals("user@mail.com", response.getUsername());
    }

    @Test
    void validateThrowsWhenTokenInvalid() {
        when(jwtTokenProvider.validateToken("bad")).thenReturn(false);

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> tokenService.validate("bad")
        );
        assertEquals("Invalid or expired token", exception.getMessage());
        verify(jwtTokenProvider, never()).getUsernameFromToken(anyString());
    }

    @Test
    void validateWrapsUnexpectedExceptionsAsInvalidToken() {
        when(jwtTokenProvider.validateToken("oops")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("oops")).thenThrow(new RuntimeException("boom"));

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> tokenService.validate("oops")
        );
        assertEquals("Invalid or expired token", exception.getMessage());
    }

    @Test
    void refreshThrowsWhenTokenNotFound() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> tokenService.refresh("missing"));
    }

    @Test
    void refreshThrowsAndDeletesWhenExpired() {
        RefreshToken expired = RefreshToken.builder()
                .id("rt-1")
                .token("expired-token")
                .userId("user-1")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        assertThrows(RefreshTokenExpiredException.class, () -> tokenService.refresh("expired-token"));
        verify(refreshTokenRepository).delete(expired);
    }

    @Test
    void refreshThrowsWhenUserMissing() {
        RefreshToken existing = RefreshToken.builder()
                .id("rt-1")
                .token("valid")
                .userId("ghost")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(refreshTokenRepository.findByToken("valid")).thenReturn(Optional.of(existing));
        when(userRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> tokenService.refresh("valid"));
        verify(refreshTokenRepository).delete(existing);
    }

    @Test
    void refreshRotatesRefreshTokenAndIssuesNewAccessToken() {
        RefreshToken existing = RefreshToken.builder()
                .id("rt-1")
                .token("old-refresh")
                .userId("user-1")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        User user = new User();
        user.setId("user-1");
        user.setName("User");
        user.setEmail("user@mail.com");
        user.setPassword("stored");
        user.setRole(Role.BURUH);

        when(refreshTokenRepository.findByToken("old-refresh")).thenReturn(Optional.of(existing));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken("user-1", Role.BURUH)).thenReturn("new-jwt");

        AuthResponse response = tokenService.refresh("old-refresh");

        assertEquals("new-jwt", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(refreshTokenRepository).delete(existing);
    }

    @Test
    void revokeDeletesWhenPresent() {
        RefreshToken existing = RefreshToken.builder()
                .id("rt-1")
                .token("to-delete")
                .userId("user-1")
                .build();

        when(refreshTokenRepository.findByToken("to-delete")).thenReturn(Optional.of(existing));

        tokenService.revoke("to-delete");

        verify(refreshTokenRepository).delete(existing);
    }

    @Test
    void revokeNoOpsWhenAbsent() {
        when(refreshTokenRepository.findByToken("nope")).thenReturn(Optional.empty());

        tokenService.revoke("nope");

        verify(refreshTokenRepository, never()).delete(any());
    }
}

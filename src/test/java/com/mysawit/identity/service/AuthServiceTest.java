package com.mysawit.identity.service;

import com.mysawit.identity.dto.AuthResponse;
import com.mysawit.identity.dto.GoogleLoginRequest;
import com.mysawit.identity.dto.LoginRequest;
import com.mysawit.identity.dto.RegisterRequest;
import com.mysawit.identity.dto.ValidateTokenResponse;
import com.mysawit.identity.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private LocalAuthService localAuthService;
    private GoogleAuthService googleAuthService;
    private AccountLinkingService accountLinkingService;
    private TokenService tokenService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        localAuthService = mock(LocalAuthService.class);
        googleAuthService = mock(GoogleAuthService.class);
        accountLinkingService = mock(AccountLinkingService.class);
        tokenService = mock(TokenService.class);

        authService = new AuthService(
                localAuthService,
                googleAuthService,
                accountLinkingService,
                tokenService
        );
    }

    @Test
    void registerDelegatesToLocalAuthService() {
        RegisterRequest request = new RegisterRequest();
        AuthResponse expected = new AuthResponse("jwt", "refresh", "1", "u", "u@mail.com", "BURUH");
        when(localAuthService.register(request)).thenReturn(expected);

        assertSame(expected, authService.register(request));
        verify(localAuthService).register(request);
        verifyNoInteractions(googleAuthService, accountLinkingService, tokenService);
    }

    @Test
    void loginDelegatesToLocalAuthService() {
        LoginRequest request = new LoginRequest();
        AuthResponse expected = new AuthResponse("jwt", "refresh", "1", "u", "u@mail.com", "BURUH");
        when(localAuthService.login(request)).thenReturn(expected);

        assertSame(expected, authService.login(request));
        verify(localAuthService).login(request);
        verifyNoInteractions(googleAuthService, accountLinkingService, tokenService);
    }

    @Test
    void googleLoginDelegatesToGoogleAuthService() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        AuthResponse expected = new AuthResponse("jwt", "refresh", "1", "u", "u@mail.com", "BURUH");
        when(googleAuthService.googleLogin(request)).thenReturn(expected);

        assertSame(expected, authService.googleLogin(request));
        verify(googleAuthService).googleLogin(request);
        verifyNoInteractions(localAuthService, accountLinkingService, tokenService);
    }

    @Test
    void validateTokenDelegatesToTokenService() {
        ValidateTokenResponse expected = new ValidateTokenResponse(true, "user@mail.com");
        when(tokenService.validate("v")).thenReturn(expected);

        assertSame(expected, authService.validateToken("v"));
        verify(tokenService).validate("v");
        verifyNoInteractions(localAuthService, googleAuthService, accountLinkingService);
    }

    @Test
    void validateTokenPropagatesInvalidTokenException() {
        when(tokenService.validate("bad")).thenThrow(new InvalidTokenException("Invalid or expired token"));

        assertThrows(InvalidTokenException.class, () -> authService.validateToken("bad"));
    }

    @Test
    void refreshTokenDelegatesToTokenService() {
        AuthResponse expected = new AuthResponse("jwt", "refresh", "10", "user", "user@mail.com", "BURUH");
        when(tokenService.refresh("rt")).thenReturn(expected);

        assertSame(expected, authService.refreshToken("rt"));
        verify(tokenService).refresh("rt");
        verifyNoInteractions(localAuthService, googleAuthService, accountLinkingService);
    }

    @Test
    void logoutDelegatesToTokenService() {
        authService.logout("rt");

        verify(tokenService).revoke("rt");
        verifyNoInteractions(localAuthService, googleAuthService, accountLinkingService);
    }

    @Test
    void linkGoogleDelegatesToAccountLinkingService() {
        authService.linkGoogle("user-1", "id-token");

        verify(accountLinkingService).linkGoogle("user-1", "id-token");
        verifyNoInteractions(localAuthService, googleAuthService, tokenService);
    }

    @Test
    void setPasswordDelegatesToAccountLinkingService() {
        authService.setPassword("user-1", "newpass");

        verify(accountLinkingService).setPassword("user-1", "newpass");
        verifyNoInteractions(localAuthService, googleAuthService, tokenService);
    }
}

package com.mysawit.identity.controller;

import com.mysawit.identity.dto.AuthResponse;
import com.mysawit.identity.dto.GoogleLoginRequest;
import com.mysawit.identity.dto.LinkGoogleRequest;
import com.mysawit.identity.dto.LoginRequest;
import com.mysawit.identity.dto.MessageResponse;
import com.mysawit.identity.dto.RefreshTokenRequest;
import com.mysawit.identity.dto.RegisterRequest;
import com.mysawit.identity.dto.SetPasswordRequest;
import com.mysawit.identity.dto.ValidateTokenRequest;
import com.mysawit.identity.dto.ValidateTokenResponse;
import com.mysawit.identity.exception.InvalidCredentialsException;
import com.mysawit.identity.exception.MissingGoogleRegistrationFieldException;
import com.mysawit.identity.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private AuthService authService;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        authController = new AuthController(authService);
    }

    @Test
    void registerReturnsSuccessResponse() {
        RegisterRequest request = new RegisterRequest();
        AuthResponse responseBody = new AuthResponse("token", "refresh", "1", "user", "user@mail.com", "BURUH");
        when(authService.register(request)).thenReturn(responseBody);

        ResponseEntity<AuthResponse> response = authController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(responseBody, response.getBody());
    }

    @Test
    void registerPropagatesException() {
        RegisterRequest request = new RegisterRequest();
        when(authService.register(request)).thenThrow(new RuntimeException("exists"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authController.register(request));

        assertEquals("exists", exception.getMessage());
    }

    @Test
    void loginReturnsSuccessResponse() {
        LoginRequest request = new LoginRequest();
        AuthResponse responseBody = new AuthResponse("token", "refresh", "1", "user", "user@mail.com", "BURUH");
        when(authService.login(request)).thenReturn(responseBody);

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(responseBody, response.getBody());
    }

    @Test
    void loginPropagatesException() {
        LoginRequest request = new LoginRequest();
        when(authService.login(request)).thenThrow(new InvalidCredentialsException("Invalid email or password"));

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authController.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void googleLoginReturnsSuccessResponse() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        AuthResponse responseBody = new AuthResponse("token", "refresh", "1", "user", "user@mail.com", "BURUH");
        when(authService.googleLogin(request)).thenReturn(responseBody);

        ResponseEntity<AuthResponse> response = authController.googleLogin(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(responseBody, response.getBody());
    }

    @Test
    void googleLoginPropagatesMissingGoogleRegistrationFieldException() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        when(authService.googleLogin(request))
                .thenThrow(new MissingGoogleRegistrationFieldException("Role is required for new Google registration"));

        MissingGoogleRegistrationFieldException exception = assertThrows(
                MissingGoogleRegistrationFieldException.class,
                () -> authController.googleLogin(request)
        );

        assertEquals("Role is required for new Google registration", exception.getMessage());
    }

    @Test
    void validateReturnsSuccessResponse() {
        ValidateTokenRequest request = new ValidateTokenRequest();
        request.setToken("valid-token");
        ValidateTokenResponse responseBody = new ValidateTokenResponse(true, "user@mail.com");
        when(authService.validateToken("valid-token")).thenReturn(responseBody);

        ResponseEntity<ValidateTokenResponse> response = authController.validate(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(responseBody, response.getBody());
    }

    @Test
    void refreshReturnsSuccessResponse() {
        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh");
        AuthResponse responseBody = new AuthResponse("new-token", "new-refresh", "1", "user", "user@mail.com", "BURUH");
        when(authService.refreshToken("old-refresh")).thenReturn(responseBody);

        ResponseEntity<AuthResponse> response = authController.refresh(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(responseBody, response.getBody());
    }

    @Test
    void logoutReturnsSuccessResponse() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");

        ResponseEntity<MessageResponse> response = authController.logout(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Logged out successfully", response.getBody().getMessage());
        verify(authService).logout("refresh-token");
    }

    @Test
    void linkGoogleReturnsSuccessResponse() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user-1");
        LinkGoogleRequest request = new LinkGoogleRequest("google-id-token");

        ResponseEntity<MessageResponse> response = authController.linkGoogle(authentication, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Google account linked successfully", response.getBody().getMessage());
        verify(authService).linkGoogle("user-1", "google-id-token");
    }

    @Test
    void setPasswordReturnsSuccessResponse() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user-1");
        SetPasswordRequest request = new SetPasswordRequest("newpass123");

        ResponseEntity<MessageResponse> response = authController.setPassword(authentication, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password set successfully", response.getBody().getMessage());
        verify(authService).setPassword("user-1", "newpass123");
    }

    @Test
    void healthReturnsUpStatus() {
        ResponseEntity<Map<String, String>> response = authController.health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("mysawit-identity-service", response.getBody().get("service"));
    }
}

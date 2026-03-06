package com.mysawit.identity.controller;

import com.mysawit.identity.dto.AuthResponse;
import com.mysawit.identity.dto.LoginRequest;
import com.mysawit.identity.dto.RegisterRequest;
import com.mysawit.identity.dto.ValidateTokenRequest;
import com.mysawit.identity.dto.ValidateTokenResponse;
import com.mysawit.identity.exception.InvalidCredentialsException;
import com.mysawit.identity.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
        AuthResponse responseBody = new AuthResponse("token", "1", "user", "user@mail.com", "BURUH");
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
        AuthResponse responseBody = new AuthResponse("token", "1", "user", "user@mail.com", "BURUH");
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
    void healthReturnsUpStatus() {
        ResponseEntity<Map<String, String>> response = authController.health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("mysawit-identity-service", response.getBody().get("service"));
    }
}

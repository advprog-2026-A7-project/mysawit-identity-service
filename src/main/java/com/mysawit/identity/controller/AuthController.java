package com.mysawit.identity.controller;

import com.mysawit.identity.dto.AuthResponse;
import com.mysawit.identity.dto.LinkGoogleRequest;
import com.mysawit.identity.dto.LoginRequest;
import com.mysawit.identity.dto.MessageResponse;
import com.mysawit.identity.dto.RefreshTokenRequest;
import com.mysawit.identity.dto.RegisterRequest;
import com.mysawit.identity.dto.SetPasswordRequest;
import com.mysawit.identity.dto.ValidateTokenRequest;
import com.mysawit.identity.dto.ValidateTokenResponse;
import com.mysawit.identity.dto.GoogleLoginRequest;
import com.mysawit.identity.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.googleLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidateTokenResponse> validate(@Valid @RequestBody ValidateTokenRequest request) {
        ValidateTokenResponse response = authService.validateToken(request.getToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    @PostMapping("/link-google")
    public ResponseEntity<MessageResponse> linkGoogle(
            Authentication authentication,
            @Valid @RequestBody LinkGoogleRequest request
    ) {
        String userId = authentication.getName();
        authService.linkGoogle(userId, request.getIdToken());
        return ResponseEntity.ok(new MessageResponse("Google account linked successfully"));
    }

    @PostMapping("/set-password")
    public ResponseEntity<MessageResponse> setPassword(
            Authentication authentication,
            @Valid @RequestBody SetPasswordRequest request
    ) {
        String userId = authentication.getName();
        authService.setPassword(userId, request.getPassword());
        return ResponseEntity.ok(new MessageResponse("Password set successfully"));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "mysawit-identity-service");
        return ResponseEntity.ok(health);
    }
}

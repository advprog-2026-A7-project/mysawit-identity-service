package com.mysawit.identity.service;

import com.mysawit.identity.dto.AuthResponse;
import com.mysawit.identity.dto.GoogleLoginRequest;
import com.mysawit.identity.dto.LoginRequest;
import com.mysawit.identity.dto.RegisterRequest;
import com.mysawit.identity.dto.ValidateTokenResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final LocalAuthService localAuthService;
    private final GoogleAuthService googleAuthService;
    private final AccountLinkingService accountLinkingService;
    private final TokenService tokenService;

    public AuthService(
            LocalAuthService localAuthService,
            GoogleAuthService googleAuthService,
            AccountLinkingService accountLinkingService,
            TokenService tokenService
    ) {
        this.localAuthService = localAuthService;
        this.googleAuthService = googleAuthService;
        this.accountLinkingService = accountLinkingService;
        this.tokenService = tokenService;
    }

    public AuthResponse register(RegisterRequest request) {
        return localAuthService.register(request);
    }

    public AuthResponse login(LoginRequest request) {
        return localAuthService.login(request);
    }

    public AuthResponse googleLogin(GoogleLoginRequest request) {
        return googleAuthService.googleLogin(request);
    }

    public ValidateTokenResponse validateToken(String token) {
        return tokenService.validate(token);
    }

    public AuthResponse refreshToken(String refreshTokenValue) {
        return tokenService.refresh(refreshTokenValue);
    }

    public void logout(String refreshTokenValue) {
        tokenService.revoke(refreshTokenValue);
    }

    public void linkGoogle(String userId, String idToken) {
        accountLinkingService.linkGoogle(userId, idToken);
    }

    public void setPassword(String userId, String rawPassword) {
        accountLinkingService.setPassword(userId, rawPassword);
    }
}

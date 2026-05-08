package com.mysawit.identity.service;

import com.mysawit.identity.dto.AuthResponse;
import com.mysawit.identity.dto.ValidateTokenResponse;
import com.mysawit.identity.exception.InvalidTokenException;
import com.mysawit.identity.exception.RefreshTokenExpiredException;
import com.mysawit.identity.model.RefreshToken;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.RefreshTokenRepository;
import com.mysawit.identity.repository.UserRepository;
import com.mysawit.identity.security.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public TokenService(
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    public AuthResponse issueTokens(User user) {
        String token = jwtTokenProvider.generateToken(user.getId(), user.getRole());
        String refreshTokenValue = jwtTokenProvider.generateRefreshTokenValue();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .userId(user.getId())
                .expiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshExpiration() / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);

        AuthResponse response = new AuthResponse(
                token,
                refreshTokenValue,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );

        response.setGoogleLinked(StringUtils.hasText(user.getGoogleSub()));
        response.setHasPassword(StringUtils.hasText(user.getPassword()));
        return response;
    }

    public ValidateTokenResponse validate(String token) {
        try {
            boolean isValid = jwtTokenProvider.validateToken(token);
            if (!isValid) {
                throw new InvalidTokenException("Invalid or expired token");
            }

            String username = jwtTokenProvider.getUsernameFromToken(token);
            return new ValidateTokenResponse(true, username);
        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("Invalid or expired token");
        }
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RefreshTokenExpiredException("Refresh token has expired");
        }

        refreshTokenRepository.delete(refreshToken);

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new InvalidTokenException("User not found for refresh token"));

        return issueTokens(user);
    }

    @Transactional
    public void revoke(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(refreshTokenRepository::delete);
    }
}

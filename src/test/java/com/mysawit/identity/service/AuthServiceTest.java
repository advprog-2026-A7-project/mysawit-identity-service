package com.mysawit.identity.service;

import com.mysawit.identity.dto.AuthResponse;
import com.mysawit.identity.dto.LoginRequest;
import com.mysawit.identity.dto.RegisterRequest;
import com.mysawit.identity.dto.ValidateTokenResponse;
import com.mysawit.identity.enums.Role;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.UserRepository;
import com.mysawit.identity.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    void registerThrowsWhenEmailExists() {
        RegisterRequest request = registerRequest();
        when(userRepository.existsByEmail("user@mail.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(request));

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerThrowsWhenRoleIsAdmin() {
        RegisterRequest request = registerRequest();
        request.setRole(Role.ADMIN);
        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(request));

        assertEquals("Cannot self-register as ADMIN", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerCreatesUserAndReturnsTokenWithDefaultRole() {
        RegisterRequest request = registerRequest();
        User saved = new User();
        saved.setId("10");
        saved.setUsername("user");
        saved.setName("user");
        saved.setEmail("user@mail.com");
        saved.setRole(Role.BURUH);

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtTokenProvider.generateToken("user", "10", Role.BURUH)).thenReturn("jwt");

        AuthResponse response = authService.register(request);

        assertEquals("jwt", response.getToken());
        assertEquals("10", response.getId());
        assertEquals("user", response.getUsername());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("encoded", captor.getValue().getPassword());
        assertEquals(Role.BURUH, captor.getValue().getRole());
    }

    @Test
    void loginThrowsWhenUserMissing() {
        LoginRequest request = loginRequest();
        when(userRepository.findByEmail("user@mail.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(request));

        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    void loginThrowsWhenPasswordMismatch() {
        LoginRequest request = loginRequest();
        User user = new User();
        user.setUsername("user");
        user.setPassword("stored");
        when(userRepository.findByEmail("user@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "stored")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(request));

        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    void loginReturnsAuthResponseWhenValid() {
        LoginRequest request = loginRequest();
        User user = new User();
        user.setId("10");
        user.setUsername("user");
        user.setName("user");
        user.setEmail("user@mail.com");
        user.setPassword("stored");
        user.setRole(Role.BURUH);

        when(userRepository.findByEmail("user@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "stored")).thenReturn(true);
        when(jwtTokenProvider.generateToken("user", "10", Role.BURUH)).thenReturn("jwt");

        AuthResponse response = authService.login(request);

        assertEquals("jwt", response.getToken());
        assertEquals("10", response.getId());
        assertEquals("user@mail.com", response.getEmail());
    }

    @Test
    void validateTokenReturnsResponseWhenTokenValid() {
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("valid-token")).thenReturn("user@mail.com");

        ValidateTokenResponse response = authService.validateToken("valid-token");

        assertTrue(response.isValid());
        assertEquals("user@mail.com", response.getUsername());
    }

    @Test
    void validateTokenThrowsWhenTokenInvalid() {
        when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.validateToken("invalid-token")
        );

        assertEquals("Invalid or expired token", exception.getMessage());
        verify(jwtTokenProvider, never()).getUsernameFromToken(anyString());
    }

    @Test
    void validateTokenThrowsWhenExtractionFails() {
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("valid-token"))
                .thenThrow(new RuntimeException("parse failed"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.validateToken("valid-token")
        );

        assertEquals("Invalid or expired token", exception.getMessage());
    }

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("user");
        request.setEmail("user@mail.com");
        request.setPassword("secret123");
        return request;
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("user@mail.com");
        request.setPassword("secret123");
        return request;
    }
}

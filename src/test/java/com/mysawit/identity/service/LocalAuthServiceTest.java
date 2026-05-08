package com.mysawit.identity.service;

import com.mysawit.identity.dto.AuthResponse;
import com.mysawit.identity.dto.LoginRequest;
import com.mysawit.identity.dto.RegisterRequest;
import com.mysawit.identity.enums.Role;
import com.mysawit.identity.event.UserRegisteredEvent;
import com.mysawit.identity.exception.DuplicateCertificationNumberException;
import com.mysawit.identity.exception.DuplicateEmailException;
import com.mysawit.identity.exception.InvalidCredentialsException;
import com.mysawit.identity.exception.InvalidRoleRegistrationException;
import com.mysawit.identity.model.Buruh;
import com.mysawit.identity.model.Mandor;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.UserRepository;
import com.mysawit.identity.service.registration.UserCreationContext;
import com.mysawit.identity.service.registration.UserRegistrationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LocalAuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private ApplicationEventPublisher eventPublisher;
    private UserRegistrationFactory userRegistrationFactory;
    private TokenService tokenService;
    private LocalAuthService localAuthService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        userRegistrationFactory = mock(UserRegistrationFactory.class);
        tokenService = mock(TokenService.class);

        localAuthService = new LocalAuthService(
                userRepository,
                passwordEncoder,
                eventPublisher,
                userRegistrationFactory,
                tokenService
        );
    }

    private AuthResponse stubIssuedTokens(String userId, Role role, String token) {
        AuthResponse response = new AuthResponse(token, "refresh-token", userId, "user", "user@mail.com", role.name());
        when(tokenService.issueTokens(argThat(u -> u != null && userId.equals(u.getId())))).thenReturn(response);
        return response;
    }

    @Test
    void registerThrowsWhenEmailExists() {
        RegisterRequest request = registerRequest();
        when(userRepository.existsByEmail("user@mail.com")).thenReturn(true);

        DuplicateEmailException exception = assertThrows(DuplicateEmailException.class, () -> localAuthService.register(request));

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(userRegistrationFactory, tokenService);
    }

    @Test
    void registerThrowsWhenRoleIsAdmin() {
        RegisterRequest request = registerRequest();
        request.setRole(Role.ADMIN);
        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);

        InvalidRoleRegistrationException exception = assertThrows(
                InvalidRoleRegistrationException.class,
                () -> localAuthService.register(request)
        );

        assertEquals("Cannot self-register as ADMIN", exception.getMessage());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(userRegistrationFactory, tokenService);
    }

    @Test
    void registerDefaultsRoleToBuruhAndPopulatesUser() {
        RegisterRequest request = registerRequest();
        Buruh built = new Buruh();
        Buruh saved = new Buruh();
        saved.setId("10");

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRegistrationFactory.create(eq(Role.BURUH), any(UserCreationContext.class))).thenReturn(built);
        when(userRepository.save(built)).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            saved.setUsername(user.getUsername());
            saved.setName(user.getName());
            saved.setEmail(user.getEmail());
            saved.setPassword(user.getPassword());
            saved.setRole(user.getRole());
            return saved;
        });
        AuthResponse expected = stubIssuedTokens("10", Role.BURUH, "jwt");

        AuthResponse response = localAuthService.register(request);

        assertSame(expected, response);
        ArgumentCaptor<UserCreationContext> ctxCaptor = ArgumentCaptor.forClass(UserCreationContext.class);
        verify(userRegistrationFactory).create(eq(Role.BURUH), ctxCaptor.capture());
        UserCreationContext ctx = ctxCaptor.getValue();
        assertNull(ctx.getCertificationNumber());
        assertNull(ctx.getMandorId());
        assertNull(ctx.getKebunId());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User toSave = userCaptor.getValue();
        assertEquals("user", toSave.getUsername());
        assertEquals("user", toSave.getName());
        assertEquals("user@mail.com", toSave.getEmail());
        assertEquals("encoded", toSave.getPassword());
        assertEquals(Role.BURUH, toSave.getRole());

        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void registerForwardsAllContextFieldsToFactory() {
        RegisterRequest request = registerRequest();
        request.setRole(Role.MANDOR);
        request.setCertificationNumber("CERT-001");
        request.setMandorId("mandor-1");
        request.setKebunId("kebun-1");

        Mandor built = new Mandor();
        Mandor saved = new Mandor();
        saved.setId("11");
        saved.setRole(Role.MANDOR);
        saved.setEmail("user@mail.com");

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRegistrationFactory.create(eq(Role.MANDOR), any(UserCreationContext.class))).thenReturn(built);
        when(userRepository.save(built)).thenReturn(saved);
        stubIssuedTokens("11", Role.MANDOR, "jwt");

        localAuthService.register(request);

        ArgumentCaptor<UserCreationContext> ctxCaptor = ArgumentCaptor.forClass(UserCreationContext.class);
        verify(userRegistrationFactory).create(eq(Role.MANDOR), ctxCaptor.capture());
        UserCreationContext ctx = ctxCaptor.getValue();
        assertEquals("CERT-001", ctx.getCertificationNumber());
        assertEquals("mandor-1", ctx.getMandorId());
        assertEquals("kebun-1", ctx.getKebunId());
    }

    @Test
    void registerPropagatesExceptionFromFactory() {
        RegisterRequest request = registerRequest();
        request.setRole(Role.MANDOR);
        request.setCertificationNumber("CERT-DUP");

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRegistrationFactory.create(eq(Role.MANDOR), any(UserCreationContext.class)))
                .thenThrow(new DuplicateCertificationNumberException("Certification number already exists"));

        assertThrows(DuplicateCertificationNumberException.class, () -> localAuthService.register(request));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(tokenService);
    }

    @Test
    void registerRethrowsDataIntegrityViolationForNonMandorRole() {
        RegisterRequest request = registerRequest();
        request.setRole(Role.BURUH);

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRegistrationFactory.create(eq(Role.BURUH), any(UserCreationContext.class))).thenReturn(new Buruh());
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("constraint"));

        assertThrows(DataIntegrityViolationException.class, () -> localAuthService.register(request));
        verifyNoInteractions(tokenService);
    }

    @Test
    void registerWrapsDataIntegrityViolationForMandorRole() {
        RegisterRequest request = registerRequest();
        request.setRole(Role.MANDOR);
        request.setCertificationNumber("CERT-001");

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRegistrationFactory.create(eq(Role.MANDOR), any(UserCreationContext.class))).thenReturn(new Mandor());
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("constraint"));

        DuplicateCertificationNumberException exception = assertThrows(
                DuplicateCertificationNumberException.class,
                () -> localAuthService.register(request)
        );
        assertEquals("Certification number already exists", exception.getMessage());
    }

    @Test
    void loginThrowsWhenUserMissing() {
        LoginRequest request = loginRequest();
        when(userRepository.findByEmail("user@mail.com")).thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> localAuthService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());
        verifyNoInteractions(tokenService);
    }

    @Test
    void loginThrowsWhenPasswordMismatch() {
        LoginRequest request = loginRequest();
        User user = new User();
        user.setUsername("user");
        user.setPassword("stored");
        when(userRepository.findByEmail("user@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "stored")).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> localAuthService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());
        verifyNoInteractions(tokenService);
    }

    @Test
    void loginIssuesTokensWhenValid() {
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
        AuthResponse expected = new AuthResponse("jwt", "refresh", "10", "user", "user@mail.com", "BURUH");
        when(tokenService.issueTokens(user)).thenReturn(expected);

        AuthResponse response = localAuthService.login(request);

        assertSame(expected, response);
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
        request.setEmail("user@mail.com");
        request.setPassword("secret123");
        return request;
    }
}

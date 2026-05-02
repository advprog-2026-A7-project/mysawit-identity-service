package com.mysawit.identity.service;

import com.mysawit.identity.dto.AuthResponse;
import com.mysawit.identity.dto.GoogleLoginRequest;
import com.mysawit.identity.dto.GoogleUserInfo;
import com.mysawit.identity.dto.LoginRequest;
import com.mysawit.identity.dto.RegisterRequest;
import com.mysawit.identity.dto.ValidateTokenResponse;
import com.mysawit.identity.enums.Role;
import com.mysawit.identity.event.UserRegisteredEvent;
import com.mysawit.identity.exception.DuplicateCertificationNumberException;
import com.mysawit.identity.exception.DuplicateEmailException;
import com.mysawit.identity.exception.GoogleSubAlreadyLinkedException;
import com.mysawit.identity.exception.InvalidCredentialsException;
import com.mysawit.identity.exception.InvalidRoleRegistrationException;
import com.mysawit.identity.exception.InvalidTokenException;
import com.mysawit.identity.exception.MissingGoogleRegistrationFieldException;
import com.mysawit.identity.exception.UserNotFoundException;
import com.mysawit.identity.model.Buruh;
import com.mysawit.identity.model.Mandor;
import com.mysawit.identity.model.Supir;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private GoogleTokenVerifierService googleTokenVerifierService;
    private ApplicationEventPublisher eventPublisher;
    private UserRegistrationFactory userRegistrationFactory;
    private TokenService tokenService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        googleTokenVerifierService = mock(GoogleTokenVerifierService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        userRegistrationFactory = mock(UserRegistrationFactory.class);
        tokenService = mock(TokenService.class);

        authService = new AuthService(
                userRepository,
                passwordEncoder,
                googleTokenVerifierService,
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

        DuplicateEmailException exception = assertThrows(DuplicateEmailException.class, () -> authService.register(request));

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
                () -> authService.register(request)
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

        AuthResponse response = authService.register(request);

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

        authService.register(request);

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

        assertThrows(DuplicateCertificationNumberException.class, () -> authService.register(request));

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

        assertThrows(DataIntegrityViolationException.class, () -> authService.register(request));
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
                () -> authService.register(request)
        );
        assertEquals("Certification number already exists", exception.getMessage());
    }

    @Test
    void loginThrowsWhenUserMissing() {
        LoginRequest request = loginRequest();
        when(userRepository.findByEmail("user@mail.com")).thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
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
                () -> authService.login(request)
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

        AuthResponse response = authService.login(request);

        assertSame(expected, response);
    }

    @Test
    void validateTokenDelegatesToTokenService() {
        ValidateTokenResponse expected = new ValidateTokenResponse(true, "user@mail.com");
        when(tokenService.validate("v")).thenReturn(expected);

        assertSame(expected, authService.validateToken("v"));
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
    }

    @Test
    void logoutDelegatesToTokenService() {
        authService.logout("rt");

        verify(tokenService).revoke("rt");
    }

    @Test
    void googleLoginReturnsExistingUserByGoogleSub() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-1")
                .email("user@mail.com")
                .name("User")
                .build();

        User existingUser = new User();
        existingUser.setId("10");
        existingUser.setName("User");
        existingUser.setEmail("user@mail.com");
        existingUser.setRole(Role.BURUH);
        existingUser.setGoogleSub("google-sub-1");

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-1")).thenReturn(Optional.of(existingUser));
        AuthResponse expected = new AuthResponse("jwt", "refresh", "10", "User", "user@mail.com", "BURUH");
        when(tokenService.issueTokens(existingUser)).thenReturn(expected);

        AuthResponse response = authService.googleLogin(request);

        assertSame(expected, response);
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(userRegistrationFactory);
    }

    @Test
    void googleLoginLinksExistingUserByEmail() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-1")
                .email("existing@mail.com")
                .name("Existing User")
                .build();

        User existingUser = new User();
        existingUser.setId("10");
        existingUser.setName("Existing User");
        existingUser.setEmail("existing@mail.com");
        existingUser.setPassword("stored");
        existingUser.setRole(Role.BURUH);

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("existing@mail.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        AuthResponse expected = new AuthResponse("jwt", "refresh", "10", "Existing User", "existing@mail.com", "BURUH");
        when(tokenService.issueTokens(existingUser)).thenReturn(expected);

        AuthResponse response = authService.googleLogin(request);

        assertSame(expected, response);
        assertEquals("google-sub-1", existingUser.getGoogleSub());
        verify(userRepository).save(existingUser);
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(userRegistrationFactory);
    }

    @Test
    void googleLoginExistingUserAlreadyLinkedSkipsSave() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-1")
                .email("existing@mail.com")
                .name("Existing User")
                .build();

        User existingUser = new User();
        existingUser.setId("10");
        existingUser.setEmail("existing@mail.com");
        existingUser.setRole(Role.BURUH);
        existingUser.setGoogleSub("google-sub-1");

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("existing@mail.com")).thenReturn(Optional.of(existingUser));
        when(tokenService.issueTokens(existingUser))
                .thenReturn(new AuthResponse("jwt", "refresh", "10", "x", "existing@mail.com", "BURUH"));

        authService.googleLogin(request);

        verify(userRepository, never()).save(any());
    }

    @Test
    void googleLoginThrowsWhenNewUserMissingRole() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");
        request.setUsername("someuser");

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-new")
                .email("new@mail.com")
                .name("New User")
                .build();

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-new")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());

        MissingGoogleRegistrationFieldException exception = assertThrows(
                MissingGoogleRegistrationFieldException.class,
                () -> authService.googleLogin(request)
        );

        assertEquals("Role is required for new Google registration", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void googleLoginThrowsWhenNewUserMissingUsername() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");
        request.setRole(Role.BURUH);

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-new")
                .email("new@mail.com")
                .name("New User")
                .build();

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-new")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());

        MissingGoogleRegistrationFieldException exception = assertThrows(
                MissingGoogleRegistrationFieldException.class,
                () -> authService.googleLogin(request)
        );

        assertEquals("Username is required for new Google registration", exception.getMessage());
    }

    @Test
    void googleLoginThrowsWhenNewUserRoleIsAdmin() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");
        request.setUsername("adminuser");
        request.setRole(Role.ADMIN);

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-new")
                .email("new@mail.com")
                .name("New User")
                .build();

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-new")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());

        InvalidRoleRegistrationException exception = assertThrows(
                InvalidRoleRegistrationException.class,
                () -> authService.googleLogin(request)
        );

        assertEquals("Cannot self-register as ADMIN", exception.getMessage());
        verifyNoInteractions(userRegistrationFactory);
    }

    @Test
    void googleLoginRegistersNewBuruhUsingFactoryAndForwardsCertificationNumber() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");
        request.setUsername("brandnewuser");
        request.setRole(Role.BURUH);

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-3")
                .email("brand-new@mail.com")
                .name("Brand New User")
                .build();

        Buruh built = new Buruh();
        Buruh saved = new Buruh();
        saved.setId("30");
        saved.setName("Brand New User");
        saved.setEmail("brand-new@mail.com");
        saved.setRole(Role.BURUH);
        saved.setGoogleSub("google-sub-3");

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-3")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("brand-new@mail.com")).thenReturn(Optional.empty());
        when(userRegistrationFactory.create(eq(Role.BURUH), any(UserCreationContext.class))).thenReturn(built);
        when(userRepository.save(built)).thenReturn(saved);
        AuthResponse expected = new AuthResponse("jwt", "refresh", "30", "brandnewuser", "brand-new@mail.com", "BURUH");
        when(tokenService.issueTokens(saved)).thenReturn(expected);

        AuthResponse response = authService.googleLogin(request);

        assertSame(expected, response);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User toSave = userCaptor.getValue();
        assertEquals("brand-new@mail.com", toSave.getEmail());
        assertEquals("Brand New User", toSave.getName());
        assertEquals("brandnewuser", toSave.getUsername());
        assertNull(toSave.getPassword());
        assertEquals(Role.BURUH, toSave.getRole());
        assertEquals("google-sub-3", toSave.getGoogleSub());

        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void googleLoginUsesEmailWhenGoogleNameIsNull() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");
        request.setUsername("newuser");
        request.setRole(Role.SUPIR);

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-2")
                .email("new@mail.com")
                .name(null)
                .build();

        Supir built = new Supir();
        Supir saved = new Supir();
        saved.setId("20");
        saved.setRole(Role.SUPIR);
        saved.setEmail("new@mail.com");

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-2")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());
        when(userRegistrationFactory.create(eq(Role.SUPIR), any(UserCreationContext.class))).thenReturn(built);
        when(userRepository.save(built)).thenReturn(saved);
        when(tokenService.issueTokens(saved))
                .thenReturn(new AuthResponse("jwt", "refresh", "20", "newuser", "new@mail.com", "SUPIR"));

        authService.googleLogin(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("new@mail.com", userCaptor.getValue().getName());
    }

    @Test
    void googleLoginRegistersNewMandorWithCertificationContext() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");
        request.setUsername("mandoruser");
        request.setRole(Role.MANDOR);
        request.setCertificationNumber("CERT-G001");

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-mandor")
                .email("mandor@mail.com")
                .name("Mandor User")
                .build();

        Mandor built = new Mandor();
        Mandor saved = new Mandor();
        saved.setId("40");
        saved.setRole(Role.MANDOR);
        saved.setEmail("mandor@mail.com");

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-mandor")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("mandor@mail.com")).thenReturn(Optional.empty());
        when(userRegistrationFactory.create(eq(Role.MANDOR), any(UserCreationContext.class))).thenReturn(built);
        when(userRepository.save(built)).thenReturn(saved);
        when(tokenService.issueTokens(saved))
                .thenReturn(new AuthResponse("jwt-mandor", "refresh", "40", "mandoruser", "mandor@mail.com", "MANDOR"));

        authService.googleLogin(request);

        ArgumentCaptor<UserCreationContext> ctxCaptor = ArgumentCaptor.forClass(UserCreationContext.class);
        verify(userRegistrationFactory).create(eq(Role.MANDOR), ctxCaptor.capture());
        UserCreationContext ctx = ctxCaptor.getValue();
        assertEquals("CERT-G001", ctx.getCertificationNumber());
        assertNull(ctx.getMandorId());
        assertNull(ctx.getKebunId());

        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void googleLoginPropagatesFactoryExceptionForNewUser() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");
        request.setUsername("mandoruser2");
        request.setRole(Role.MANDOR);
        request.setCertificationNumber("CERT-DUP");

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-mandor2")
                .email("mandor2@mail.com")
                .name("Mandor User 2")
                .build();

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-mandor2")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("mandor2@mail.com")).thenReturn(Optional.empty());
        when(userRegistrationFactory.create(eq(Role.MANDOR), any(UserCreationContext.class)))
                .thenThrow(new DuplicateCertificationNumberException("Certification number already exists"));

        DuplicateCertificationNumberException exception = assertThrows(
                DuplicateCertificationNumberException.class,
                () -> authService.googleLogin(request)
        );

        assertEquals("Certification number already exists", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void googleLoginWrapsDataIntegrityViolationForMandorRole() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");
        request.setUsername("mandoruser3");
        request.setRole(Role.MANDOR);
        request.setCertificationNumber("CERT-RACE");

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-mandor3")
                .email("mandor3@mail.com")
                .name("Mandor User 3")
                .build();

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-mandor3")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("mandor3@mail.com")).thenReturn(Optional.empty());
        when(userRegistrationFactory.create(eq(Role.MANDOR), any(UserCreationContext.class))).thenReturn(new Mandor());
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("constraint"));

        DuplicateCertificationNumberException exception = assertThrows(
                DuplicateCertificationNumberException.class,
                () -> authService.googleLogin(request)
        );

        assertEquals("Certification number already exists", exception.getMessage());
    }

    @Test
    void googleLoginRethrowsDataIntegrityViolationForNonMandorRole() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");
        request.setUsername("buruhuser2");
        request.setRole(Role.BURUH);

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-buruh2")
                .email("buruh2@mail.com")
                .name("Buruh User 2")
                .build();

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-buruh2")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("buruh2@mail.com")).thenReturn(Optional.empty());
        when(userRegistrationFactory.create(eq(Role.BURUH), any(UserCreationContext.class))).thenReturn(new Buruh());
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("constraint"));

        assertThrows(DataIntegrityViolationException.class, () -> authService.googleLogin(request));
    }

    @Test
    void linkGoogleSetsGoogleSub() {
        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-1")
                .email("user@mail.com")
                .name("User")
                .build();

        User user = new User();
        user.setId("user-1");
        user.setEmail("user@mail.com");

        when(googleTokenVerifierService.verifyToken("id-token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-1")).thenReturn(Optional.empty());
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        authService.linkGoogle("user-1", "id-token");

        assertEquals("google-sub-1", user.getGoogleSub());
        verify(userRepository).save(user);
    }

    @Test
    void linkGoogleThrowsWhenSubAlreadyLinkedToOtherUser() {
        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-1")
                .email("user@mail.com")
                .name("User")
                .build();

        User otherUser = new User();
        otherUser.setId("other-user");

        when(googleTokenVerifierService.verifyToken("id-token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-1")).thenReturn(Optional.of(otherUser));

        assertThrows(GoogleSubAlreadyLinkedException.class, () -> authService.linkGoogle("user-1", "id-token"));
    }

    @Test
    void linkGoogleSucceedsWhenSubAlreadyLinkedToSameUser() {
        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-1")
                .email("user@mail.com")
                .name("User")
                .build();

        User sameUser = new User();
        sameUser.setId("user-1");

        when(googleTokenVerifierService.verifyToken("id-token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-1")).thenReturn(Optional.of(sameUser));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sameUser));

        authService.linkGoogle("user-1", "id-token");

        verify(userRepository).save(sameUser);
    }

    @Test
    void linkGoogleThrowsWhenUserNotFound() {
        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-1")
                .email("user@mail.com")
                .name("User")
                .build();

        when(googleTokenVerifierService.verifyToken("id-token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-1")).thenReturn(Optional.empty());
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.linkGoogle("user-1", "id-token"));
    }

    @Test
    void setPasswordEncodesAndSaves() {
        User user = new User();
        user.setId("user-1");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("encoded-new");

        authService.setPassword("user-1", "newpass");

        assertEquals("encoded-new", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void setPasswordThrowsWhenUserNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.setPassword("missing", "newpass"));
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

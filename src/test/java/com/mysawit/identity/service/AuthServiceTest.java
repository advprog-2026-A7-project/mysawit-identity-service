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
import com.mysawit.identity.exception.InvalidMandorException;
import com.mysawit.identity.exception.InvalidRoleRegistrationException;
import com.mysawit.identity.exception.InvalidTokenException;
import com.mysawit.identity.exception.MissingGoogleRegistrationFieldException;
import com.mysawit.identity.exception.MissingMandorCertificationException;
import com.mysawit.identity.exception.RefreshTokenExpiredException;
import com.mysawit.identity.exception.UserNotFoundException;
import com.mysawit.identity.model.Buruh;
import com.mysawit.identity.model.Mandor;
import com.mysawit.identity.model.RefreshToken;
import com.mysawit.identity.model.Supir;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.MandorRepository;
import com.mysawit.identity.repository.RefreshTokenRepository;
import com.mysawit.identity.repository.UserRepository;
import com.mysawit.identity.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private MandorRepository mandorRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private GoogleTokenVerifierService googleTokenVerifierService;
    private ApplicationEventPublisher eventPublisher;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        mandorRepository = mock(MandorRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        googleTokenVerifierService = mock(GoogleTokenVerifierService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        authService = new AuthService(userRepository, mandorRepository, refreshTokenRepository,
                passwordEncoder, jwtTokenProvider, googleTokenVerifierService, eventPublisher);

        when(jwtTokenProvider.generateRefreshTokenValue()).thenReturn("refresh-token");
        when(jwtTokenProvider.getRefreshExpiration()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void registerThrowsWhenEmailExists() {
        RegisterRequest request = registerRequest();
        when(userRepository.existsByEmail("user@mail.com")).thenReturn(true);

        DuplicateEmailException exception = assertThrows(DuplicateEmailException.class, () -> authService.register(request));

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any());
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
    }

    @Test
    void registerCreatesBuruhAndReturnsTokenWithDefaultRole() {
        RegisterRequest request = registerRequest();
        Buruh saved = new Buruh();
        saved.setId("10");
        saved.setUsername("user");
        saved.setName("user");
        saved.setEmail("user@mail.com");
        saved.setPassword("encoded");
        saved.setRole(Role.BURUH);

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtTokenProvider.generateToken("10", Role.BURUH)).thenReturn("jwt");

        AuthResponse response = authService.register(request);

        assertEquals("jwt", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("10", response.getId());
        assertEquals("user", response.getUsername());
        assertFalse(response.getGoogleLinked());
        assertTrue(response.getHasPassword());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertInstanceOf(Buruh.class, captor.getValue());
        assertEquals("encoded", captor.getValue().getPassword());
        assertEquals(Role.BURUH, captor.getValue().getRole());

        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void registerCreatesMandorWhenRoleMandor() {
        RegisterRequest request = registerRequest();
        request.setRole(Role.MANDOR);
        request.setCertificationNumber("CERT-001");

        Mandor saved = new Mandor();
        saved.setId("11");
        saved.setUsername("user");
        saved.setName("user");
        saved.setEmail("user@mail.com");
        saved.setPassword("encoded");
        saved.setRole(Role.MANDOR);
        saved.setCertificationNumber("CERT-001");

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);
        when(mandorRepository.existsByCertificationNumber("CERT-001")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtTokenProvider.generateToken("11", Role.MANDOR)).thenReturn("jwt");

        AuthResponse response = authService.register(request);

        assertEquals("jwt", response.getToken());
        assertEquals("MANDOR", response.getRole());
        assertFalse(response.getGoogleLinked());
        assertTrue(response.getHasPassword());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertInstanceOf(Mandor.class, captor.getValue());
        assertEquals("CERT-001", ((Mandor) captor.getValue()).getCertificationNumber());
    }

    @Test
    void registerThrowsWhenMandorCertificationMissing() {
        RegisterRequest request = registerRequest();
        request.setRole(Role.MANDOR);
        request.setCertificationNumber(" ");
        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);

        MissingMandorCertificationException exception = assertThrows(
                MissingMandorCertificationException.class,
                () -> authService.register(request)
        );

        assertEquals("Certification number is required for MANDOR", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerThrowsWhenCertificationNumberAlreadyExists() {
        RegisterRequest request = registerRequest();
        request.setRole(Role.MANDOR);
        request.setCertificationNumber("CERT-001");

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);
        when(mandorRepository.existsByCertificationNumber("CERT-001")).thenReturn(true);

        DuplicateCertificationNumberException exception = assertThrows(
                DuplicateCertificationNumberException.class,
                () -> authService.register(request)
        );

        assertEquals("Certification number already exists", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerCreatesBuruhWithMandorWhenMandorIdValid() {
        RegisterRequest request = registerRequest();
        request.setRole(Role.BURUH);
        request.setMandorId("mandor-1");

        Mandor mandor = new Mandor();
        mandor.setId("mandor-1");
        mandor.setRole(Role.MANDOR);

        Buruh saved = new Buruh();
        saved.setId("12");
        saved.setUsername("user");
        saved.setName("user");
        saved.setEmail("user@mail.com");
        saved.setRole(Role.BURUH);
        saved.setMandor(mandor);

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);
        when(mandorRepository.findById("mandor-1")).thenReturn(Optional.of(mandor));
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtTokenProvider.generateToken("12", Role.BURUH)).thenReturn("jwt");

        AuthResponse response = authService.register(request);

        assertEquals("jwt", response.getToken());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertInstanceOf(Buruh.class, captor.getValue());
        assertNotNull(((Buruh) captor.getValue()).getMandor());
        assertEquals("mandor-1", ((Buruh) captor.getValue()).getMandor().getId());
    }

    @Test
    void registerThrowsWhenMandorIdInvalid() {
        RegisterRequest request = registerRequest();
        request.setRole(Role.BURUH);
        request.setMandorId("missing-mandor");

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);
        when(mandorRepository.findById("missing-mandor")).thenReturn(Optional.empty());

        InvalidMandorException exception = assertThrows(InvalidMandorException.class, () -> authService.register(request));

        assertEquals("Invalid mandorId", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerCreatesSupirWhenRoleSupir() {
        RegisterRequest request = registerRequest();
        request.setRole(Role.SUPIR);
        request.setKebunId("kebun-1");

        Supir saved = new Supir();
        saved.setId("13");
        saved.setUsername("user");
        saved.setName("user");
        saved.setEmail("user@mail.com");
        saved.setPassword("encoded");
        saved.setRole(Role.SUPIR);
        saved.setKebunId("kebun-1");

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtTokenProvider.generateToken("13", Role.SUPIR)).thenReturn("jwt");

        AuthResponse response = authService.register(request);

        assertEquals("SUPIR", response.getRole());
        assertFalse(response.getGoogleLinked());
        assertTrue(response.getHasPassword());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertInstanceOf(Supir.class, captor.getValue());
        assertEquals("kebun-1", ((Supir) captor.getValue()).getKebunId());
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
        when(jwtTokenProvider.generateToken("10", Role.BURUH)).thenReturn("jwt");

        AuthResponse response = authService.login(request);

        assertEquals("jwt", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("10", response.getId());
        assertEquals("user@mail.com", response.getEmail());
        assertFalse(response.getGoogleLinked());
        assertTrue(response.getHasPassword());
    }

    @Test
    void loginReturnsAuthResponseWhenAdminValid() {
        LoginRequest request = loginRequest();
        User admin = new User();
        admin.setId("99");
        admin.setUsername("admin");
        admin.setName("admin");
        admin.setEmail("user@mail.com");
        admin.setPassword("stored");
        admin.setRole(Role.ADMIN);

        when(userRepository.findByEmail("user@mail.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("secret123", "stored")).thenReturn(true);
        when(jwtTokenProvider.generateToken("99", Role.ADMIN)).thenReturn("jwt-admin");

        AuthResponse response = authService.login(request);

        assertEquals("jwt-admin", response.getToken());
        assertEquals("ADMIN", response.getRole());
        assertFalse(response.getGoogleLinked());
        assertTrue(response.getHasPassword());
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

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
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

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> authService.validateToken("valid-token")
        );

        assertEquals("Invalid or expired token", exception.getMessage());
    }

    @Test
    void registerRethrowsDataIntegrityViolationForNonMandorRole() {
        RegisterRequest request = registerRequest();
        request.setRole(Role.BURUH);

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("constraint"));

        assertThrows(DataIntegrityViolationException.class, () -> authService.register(request));
    }

    @Test
    void registerWrapsDataIntegrityViolationForMandorRole() {
        RegisterRequest request = registerRequest();
        request.setRole(Role.MANDOR);
        request.setCertificationNumber("CERT-001");

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);
        when(mandorRepository.existsByCertificationNumber("CERT-001")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("constraint"));

        DuplicateCertificationNumberException exception = assertThrows(
                DuplicateCertificationNumberException.class,
                () -> authService.register(request)
        );
        assertEquals("Certification number already exists", exception.getMessage());
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
        when(jwtTokenProvider.generateToken("10", Role.BURUH)).thenReturn("jwt");

        AuthResponse response = authService.googleLogin(request);

        assertEquals("jwt", response.getToken());
        assertTrue(response.getGoogleLinked());
        assertTrue(response.getHasPassword());
        assertEquals("google-sub-1", existingUser.getGoogleSub());
        verify(eventPublisher, never()).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void googleLoginCreatesNewUserWithNullName() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");
        request.setUsername("newuser");
        request.setRole(Role.BURUH);

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-2")
                .email("new@mail.com")
                .name(null)
                .build();

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-2")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("20");
            return saved;
        });
        when(jwtTokenProvider.generateToken("20", Role.BURUH)).thenReturn("jwt");

        AuthResponse response = authService.googleLogin(request);

        assertEquals("jwt", response.getToken());
        assertTrue(response.getGoogleLinked());
        assertFalse(response.getHasPassword());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("new@mail.com", captor.getValue().getName());
        assertEquals("newuser", captor.getValue().getUsername());
        assertNull(captor.getValue().getPassword());
        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
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
        existingUser.setPassword(null);

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-1")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.generateToken("10", Role.BURUH)).thenReturn("jwt");

        AuthResponse response = authService.googleLogin(request);

        assertEquals("jwt", response.getToken());
        assertTrue(response.getGoogleLinked());
        assertFalse(response.getHasPassword());
        verify(userRepository, never()).findByEmail(anyString());
        verify(eventPublisher, never()).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void googleLoginCreatesNewBuruhWithName() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");
        request.setUsername("brandnewuser");
        request.setRole(Role.BURUH);

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-3")
                .email("brand-new@mail.com")
                .name("Brand New User")
                .build();

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-3")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("brand-new@mail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("30");
            return saved;
        });
        when(jwtTokenProvider.generateToken("30", Role.BURUH)).thenReturn("jwt");

        AuthResponse response = authService.googleLogin(request);

        assertEquals("jwt", response.getToken());
        assertTrue(response.getGoogleLinked());
        assertFalse(response.getHasPassword());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertInstanceOf(Buruh.class, captor.getValue());
        assertEquals("Brand New User", captor.getValue().getName());
        assertEquals("brandnewuser", captor.getValue().getUsername());
        assertNull(captor.getValue().getPassword());
        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void googleLoginRegistersNewMandorWithCertification() {
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

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-mandor")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("mandor@mail.com")).thenReturn(Optional.empty());
        when(mandorRepository.existsByCertificationNumber("CERT-G001")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("40");
            return saved;
        });
        when(jwtTokenProvider.generateToken("40", Role.MANDOR)).thenReturn("jwt-mandor");

        AuthResponse response = authService.googleLogin(request);

        assertEquals("jwt-mandor", response.getToken());
        assertEquals("MANDOR", response.getRole());
        assertTrue(response.getGoogleLinked());
        assertFalse(response.getHasPassword());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertInstanceOf(Mandor.class, captor.getValue());
        assertEquals("CERT-G001", ((Mandor) captor.getValue()).getCertificationNumber());
        assertEquals("mandoruser", captor.getValue().getUsername());
        assertNull(captor.getValue().getPassword());

        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void googleLoginExistingUserIgnoresExtraFields() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");
        request.setUsername("ignored-username");
        request.setRole(Role.MANDOR);
        request.setCertificationNumber("CERT-IGNORED");

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-existing")
                .email("existing@mail.com")
                .name("Existing User")
                .build();

        User existingUser = new User();
        existingUser.setId("50");
        existingUser.setName("Existing User");
        existingUser.setEmail("existing@mail.com");
        existingUser.setPassword("stored");
        existingUser.setRole(Role.BURUH);
        existingUser.setGoogleSub("google-sub-existing");

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-existing")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.generateToken("50", Role.BURUH)).thenReturn("jwt");

        AuthResponse response = authService.googleLogin(request);

        assertEquals("jwt", response.getToken());
        assertEquals("BURUH", response.getRole());
        assertEquals("Existing User", response.getUsername());
        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(UserRegisteredEvent.class));
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
        verify(userRepository, never()).save(any());
    }

    @Test
    void googleLoginThrowsWhenMandorMissingCertification() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");
        request.setUsername("mandoruser");
        request.setRole(Role.MANDOR);

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-new")
                .email("new@mail.com")
                .name("New User")
                .build();

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-new")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());

        MissingMandorCertificationException exception = assertThrows(
                MissingMandorCertificationException.class,
                () -> authService.googleLogin(request)
        );

        assertEquals("Certification number is required for MANDOR", exception.getMessage());
        verify(userRepository, never()).save(any());
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
        verify(userRepository, never()).save(any());
    }

    @Test
    void refreshTokenReturnsNewTokens() {
        RefreshToken existing = RefreshToken.builder()
                .id("rt-1")
                .token("old-refresh")
                .userId("user-1")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        User user = new User();
        user.setId("user-1");
        user.setName("user");
        user.setEmail("user@mail.com");
        user.setPassword("stored");
        user.setRole(Role.BURUH);

        when(refreshTokenRepository.findByToken("old-refresh")).thenReturn(Optional.of(existing));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken("user-1", Role.BURUH)).thenReturn("new-jwt");

        AuthResponse response = authService.refreshToken("old-refresh");

        assertEquals("new-jwt", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertFalse(response.getGoogleLinked());
        assertTrue(response.getHasPassword());
        verify(refreshTokenRepository).delete(existing);
    }

    @Test
    void refreshTokenThrowsWhenTokenNotFound() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> authService.refreshToken("missing"));
    }

    @Test
    void refreshTokenThrowsWhenExpired() {
        RefreshToken expired = RefreshToken.builder()
                .id("rt-1")
                .token("expired-token")
                .userId("user-1")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        assertThrows(RefreshTokenExpiredException.class, () -> authService.refreshToken("expired-token"));
        verify(refreshTokenRepository).delete(expired);
    }

    @Test
    void refreshTokenThrowsWhenUserNotFound() {
        RefreshToken existing = RefreshToken.builder()
                .id("rt-1")
                .token("valid-refresh")
                .userId("deleted-user")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(refreshTokenRepository.findByToken("valid-refresh")).thenReturn(Optional.of(existing));
        when(userRepository.findById("deleted-user")).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> authService.refreshToken("valid-refresh"));
    }

    @Test
    void logoutDeletesRefreshToken() {
        RefreshToken existing = RefreshToken.builder()
                .id("rt-1")
                .token("token-to-delete")
                .userId("user-1")
                .build();

        when(refreshTokenRepository.findByToken("token-to-delete")).thenReturn(Optional.of(existing));

        authService.logout("token-to-delete");

        verify(refreshTokenRepository).delete(existing);
    }

    @Test
    void logoutDoesNothingWhenTokenNotFound() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        authService.logout("missing");

        verify(refreshTokenRepository, never()).delete(any());
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

    @Test
    void buildUserByRoleThrowsForAdminRole() throws Exception {
        Method buildUserByRole = AuthService.class.getDeclaredMethod("buildUserByRole", RegisterRequest.class, Role.class);
        buildUserByRole.setAccessible(true);

        RegisterRequest request = registerRequest();
        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> buildUserByRole.invoke(authService, request, Role.ADMIN)
        );
        assertInstanceOf(InvalidRoleRegistrationException.class, exception.getCause());
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

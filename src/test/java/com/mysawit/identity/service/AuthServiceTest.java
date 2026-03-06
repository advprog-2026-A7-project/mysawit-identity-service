package com.mysawit.identity.service;

import com.mysawit.identity.dto.AuthResponse;
import com.mysawit.identity.dto.GoogleLoginRequest;
import com.mysawit.identity.dto.GoogleUserInfo;
import com.mysawit.identity.dto.LoginRequest;
import com.mysawit.identity.dto.RegisterRequest;
import com.mysawit.identity.dto.ValidateTokenResponse;
import com.mysawit.identity.enums.Role;
import com.mysawit.identity.exception.DuplicateCertificationNumberException;
import com.mysawit.identity.exception.DuplicateEmailException;
import com.mysawit.identity.exception.InvalidCredentialsException;
import com.mysawit.identity.exception.InvalidMandorException;
import com.mysawit.identity.exception.InvalidRoleRegistrationException;
import com.mysawit.identity.exception.InvalidTokenException;
import com.mysawit.identity.exception.MissingMandorCertificationException;
import com.mysawit.identity.model.Buruh;
import com.mysawit.identity.model.Mandor;
import com.mysawit.identity.model.Supir;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.MandorRepository;
import com.mysawit.identity.repository.UserRepository;
import com.mysawit.identity.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private MandorRepository mandorRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private GoogleTokenVerifierService googleTokenVerifierService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        mandorRepository = mock(MandorRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        googleTokenVerifierService = mock(GoogleTokenVerifierService.class);
        authService = new AuthService(userRepository, mandorRepository, passwordEncoder, jwtTokenProvider, googleTokenVerifierService);
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
        saved.setRole(Role.BURUH);

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtTokenProvider.generateToken("10", Role.BURUH)).thenReturn("jwt");

        AuthResponse response = authService.register(request);

        assertEquals("jwt", response.getToken());
        assertEquals("10", response.getId());
        assertEquals("user", response.getUsername());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertInstanceOf(Buruh.class, captor.getValue());
        assertEquals("encoded", captor.getValue().getPassword());
        assertEquals(Role.BURUH, captor.getValue().getRole());
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
        saved.setRole(Role.SUPIR);
        saved.setKebunId("kebun-1");

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtTokenProvider.generateToken("13", Role.SUPIR)).thenReturn("jwt");

        AuthResponse response = authService.register(request);

        assertEquals("SUPIR", response.getRole());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertInstanceOf(Supir.class, captor.getValue());
        assertEquals("kebun-1", ((Supir) captor.getValue()).getKebunId());
    }

    @Test
    void registerThrowsDuplicateCertificationWhenMandorSaveViolatesConstraint() {
        RegisterRequest request = registerRequest();
        request.setRole(Role.MANDOR);
        request.setCertificationNumber("CERT-001");

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);
        when(mandorRepository.existsByCertificationNumber("CERT-001")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate cert"));

        DuplicateCertificationNumberException exception = assertThrows(
                DuplicateCertificationNumberException.class,
                () -> authService.register(request)
        );

        assertEquals("Certification number already exists", exception.getMessage());
    }

    @Test
    void registerRethrowsDataIntegrityViolationForNonMandorRole() {
        RegisterRequest request = registerRequest();
        request.setRole(Role.BURUH);

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("generic violation"));

        assertThrows(DataIntegrityViolationException.class, () -> authService.register(request));
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
        assertEquals("10", response.getId());
        assertEquals("user@mail.com", response.getEmail());
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
    }

    @Test
    void googleLoginUpdatesExistingUserByEmailWithGoogleSub() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("google-token");

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-1")
                .email("user@mail.com")
                .name("Google User")
                .build();

        User existingUser = new User();
        existingUser.setId("15");
        existingUser.setUsername("user");
        existingUser.setName("user");
        existingUser.setEmail("user@mail.com");
        existingUser.setRole(Role.BURUH);

        when(googleTokenVerifierService.verifyToken("google-token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@mail.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(jwtTokenProvider.generateToken("15", Role.BURUH)).thenReturn("jwt-google");

        AuthResponse response = authService.googleLogin(request);

        assertEquals("jwt-google", response.getToken());
        assertEquals("google-sub-1", existingUser.getGoogleSub());
        verify(userRepository).save(existingUser);
    }

    @Test
    void googleLoginUsesEmailAsNameWhenGoogleNameMissing() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("google-token-2");

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("google-sub-2")
                .email("new-user@mail.com")
                .name(null)
                .build();

        when(googleTokenVerifierService.verifyToken("google-token-2")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("google-sub-2")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new-user@mail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-random-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("16");
            return saved;
        });
        when(jwtTokenProvider.generateToken("16", Role.BURUH)).thenReturn("jwt-new");

        AuthResponse response = authService.googleLogin(request);

        assertEquals("jwt-new", response.getToken());
        assertEquals("new-user@mail.com", response.getUsername());
        assertEquals("new-user@mail.com", response.getEmail());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertInstanceOf(Buruh.class, captor.getValue());
        assertEquals("new-user@mail.com", captor.getValue().getName());
        assertEquals("new-user@mail.com", captor.getValue().getUsername());
        assertEquals("google-sub-2", captor.getValue().getGoogleSub());
    }

    @Test
    void buildUserByRoleThrowsWhenAdminCaseIsInvoked() throws Exception {
        RegisterRequest request = registerRequest();
        Method method = AuthService.class.getDeclaredMethod("buildUserByRole", RegisterRequest.class, Role.class);
        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(authService, request, Role.ADMIN)
        );

        assertInstanceOf(InvalidRoleRegistrationException.class, exception.getCause());
        assertEquals("Cannot self-register as ADMIN", exception.getCause().getMessage());
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

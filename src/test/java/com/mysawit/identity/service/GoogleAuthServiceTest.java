package com.mysawit.identity.service;

import com.mysawit.identity.dto.AuthResponse;
import com.mysawit.identity.dto.GoogleLoginRequest;
import com.mysawit.identity.dto.GoogleUserInfo;
import com.mysawit.identity.enums.Role;
import com.mysawit.identity.event.UserRegisteredEvent;
import com.mysawit.identity.exception.DuplicateCertificationNumberException;
import com.mysawit.identity.exception.InvalidRoleRegistrationException;
import com.mysawit.identity.exception.MissingGoogleRegistrationFieldException;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GoogleAuthServiceTest {

    private UserRepository userRepository;
    private GoogleTokenVerifierService googleTokenVerifierService;
    private ApplicationEventPublisher eventPublisher;
    private UserRegistrationFactory userRegistrationFactory;
    private TokenService tokenService;
    private GoogleAuthService googleAuthService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        googleTokenVerifierService = mock(GoogleTokenVerifierService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        userRegistrationFactory = mock(UserRegistrationFactory.class);
        tokenService = mock(TokenService.class);

        googleAuthService = new GoogleAuthService(
                userRepository,
                googleTokenVerifierService,
                eventPublisher,
                userRegistrationFactory,
                tokenService
        );
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

        AuthResponse response = googleAuthService.googleLogin(request);

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

        AuthResponse response = googleAuthService.googleLogin(request);

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

        googleAuthService.googleLogin(request);

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
                () -> googleAuthService.googleLogin(request)
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
                () -> googleAuthService.googleLogin(request)
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
                () -> googleAuthService.googleLogin(request)
        );

        assertEquals("Cannot self-register as ADMIN", exception.getMessage());
        verifyNoInteractions(userRegistrationFactory);
    }

    @Test
    void googleLoginRejectsAdminRoleBeforeMissingUsername() {
        // ADMIN check must run before the username-required check so the user
        // sees the correct error message ("Cannot self-register as ADMIN")
        // even when other fields are also missing.
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");
        request.setRole(Role.ADMIN);
        // username intentionally omitted

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
                () -> googleAuthService.googleLogin(request)
        );

        assertEquals("Cannot self-register as ADMIN", exception.getMessage());
        verifyNoInteractions(userRegistrationFactory);
        verify(userRepository, never()).save(any());
    }

    @Test
    void googleLoginIgnoresAdminMatchedByEmailWhenGoogleSubNotLinked() {
        // Security guard: an existing ADMIN must never be auto-linked to a
        // Google identity solely because the token's email matches. The lookup
        // must fall through to the (rejected) self-registration path instead.
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");
        request.setUsername("anyuser");
        request.setRole(Role.BURUH);

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("attacker-google-sub")
                .email("admin@mysawit.com")
                .name("Attacker")
                .build();

        User existingAdmin = new User();
        existingAdmin.setId("admin-1");
        existingAdmin.setEmail("admin@mysawit.com");
        existingAdmin.setRole(Role.ADMIN);
        // no googleSub linked yet

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("attacker-google-sub")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@mysawit.com")).thenReturn(Optional.of(existingAdmin));
        // Falls through to registration; BURUH email collision -> DataIntegrityViolation
        when(userRegistrationFactory.create(eq(Role.BURUH), any(UserCreationContext.class))).thenReturn(new Buruh());
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("email"));

        assertThrows(DataIntegrityViolationException.class, () -> googleAuthService.googleLogin(request));

        // The existing admin must NOT have been mutated or saved.
        assertNull(existingAdmin.getGoogleSub());
        verify(userRepository, never()).save(existingAdmin);
        verify(tokenService, never()).issueTokens(existingAdmin);
    }

    @Test
    void googleLoginStillHonoursExplicitlyLinkedAdminGoogleSub() {
        // An admin that has previously linked their Google account (e.g. via
        // /auth/link-google) MUST still be able to sign in with Google.
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("token");

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .googleSub("admin-google-sub")
                .email("admin@mysawit.com")
                .name("Admin")
                .build();

        User existingAdmin = new User();
        existingAdmin.setId("admin-1");
        existingAdmin.setEmail("admin@mysawit.com");
        existingAdmin.setRole(Role.ADMIN);
        existingAdmin.setGoogleSub("admin-google-sub");

        when(googleTokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSub("admin-google-sub")).thenReturn(Optional.of(existingAdmin));
        AuthResponse expected = new AuthResponse("jwt", "refresh", "admin-1", "Admin", "admin@mysawit.com", "ADMIN");
        when(tokenService.issueTokens(existingAdmin)).thenReturn(expected);

        AuthResponse response = googleAuthService.googleLogin(request);

        assertSame(expected, response);
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any());
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

        AuthResponse response = googleAuthService.googleLogin(request);

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

        googleAuthService.googleLogin(request);

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

        googleAuthService.googleLogin(request);

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
                () -> googleAuthService.googleLogin(request)
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
                () -> googleAuthService.googleLogin(request)
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

        assertThrows(DataIntegrityViolationException.class, () -> googleAuthService.googleLogin(request));
    }
}

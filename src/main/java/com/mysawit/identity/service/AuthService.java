package com.mysawit.identity.service;

import com.mysawit.identity.dto.AuthResponse;
import com.mysawit.identity.dto.LoginRequest;
import com.mysawit.identity.dto.RegisterRequest;
import com.mysawit.identity.dto.ValidateTokenResponse;
import com.mysawit.identity.dto.GoogleLoginRequest;
import com.mysawit.identity.dto.GoogleUserInfo;
import com.mysawit.identity.enums.Role;
import com.mysawit.identity.exception.DuplicateCertificationNumberException;
import com.mysawit.identity.exception.DuplicateEmailException;
import com.mysawit.identity.exception.GoogleSubAlreadyLinkedException;
import com.mysawit.identity.exception.InvalidCredentialsException;
import com.mysawit.identity.exception.InvalidRoleRegistrationException;
import com.mysawit.identity.exception.MissingGoogleRegistrationFieldException;
import com.mysawit.identity.exception.UserNotFoundException;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.UserRepository;
import com.mysawit.identity.service.registration.UserCreationContext;
import com.mysawit.identity.service.registration.UserRegistrationFactory;
import com.mysawit.identity.event.UserRegisteredEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRegistrationFactory userRegistrationFactory;
    private final TokenService tokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            GoogleTokenVerifierService googleTokenVerifierService,
            ApplicationEventPublisher eventPublisher,
            UserRegistrationFactory userRegistrationFactory,
            TokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.eventPublisher = eventPublisher;
        this.userRegistrationFactory = userRegistrationFactory;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Role role = validateAndResolveRegistrationRole(request);
        User user = buildAndPopulateNewUser(request, role);
        User savedUser = persistUserHandlingDuplicateCertification(user, role);
        publishUserRegisteredEvent(savedUser);
        return tokenService.issueTokens(savedUser);
    }

    private Role validateAndResolveRegistrationRole(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already exists");
        }

        Role role = request.getRole() != null ? request.getRole() : Role.BURUH;
        if (role == Role.ADMIN) {
            throw new InvalidRoleRegistrationException("Cannot self-register as ADMIN");
        }
        return role;
    }

    private User buildAndPopulateNewUser(RegisterRequest request, Role role) {
        UserCreationContext context = UserCreationContext.builder()
                .certificationNumber(request.getCertificationNumber())
                .mandorId(request.getMandorId())
                .kebunId(request.getKebunId())
                .build();
        User user = userRegistrationFactory.create(role, context);
        user.setName(request.getUsername());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        return user;
    }

    private User persistUserHandlingDuplicateCertification(User user, Role role) {
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            if (role == Role.MANDOR) {
                throw new DuplicateCertificationNumberException("Certification number already exists");
            }
            throw e;
        }
    }

    private void publishUserRegisteredEvent(User user) {
        eventPublisher.publishEvent(new UserRegisteredEvent(
                user.getId(), user.getEmail(), user.getRole().name()));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return tokenService.issueTokens(user);
    }

    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        GoogleUserInfo googleUserInfo = googleTokenVerifierService.verifyToken(request.getIdToken());

        User existingUser = findExistingUserForGoogleLogin(googleUserInfo);
        if (existingUser != null) {
            return handleExistingUserLogin(existingUser, googleUserInfo);
        }

        return registerNewGoogleUser(request, googleUserInfo);
    }

    private User findExistingUserForGoogleLogin(GoogleUserInfo googleUserInfo) {
        return userRepository.findByGoogleSub(googleUserInfo.getGoogleSub())
                .orElseGet(() -> userRepository.findByEmail(googleUserInfo.getEmail()).orElse(null));
    }

    private AuthResponse handleExistingUserLogin(User existingUser, GoogleUserInfo googleUserInfo) {
        if (existingUser.getGoogleSub() == null) {
            existingUser.setGoogleSub(googleUserInfo.getGoogleSub());
            userRepository.save(existingUser);
        }
        return tokenService.issueTokens(existingUser);
    }

    private AuthResponse registerNewGoogleUser(GoogleLoginRequest request, GoogleUserInfo googleUserInfo) {
        validateGoogleRegistrationRequest(request);
        User newUser = buildAndPopulateNewGoogleUser(request, googleUserInfo);
        User savedUser = persistUserHandlingDuplicateCertification(newUser, request.getRole());
        publishUserRegisteredEvent(savedUser);
        return tokenService.issueTokens(savedUser);
    }

    private void validateGoogleRegistrationRequest(GoogleLoginRequest request) {
        if (request.getRole() == null) {
            throw new MissingGoogleRegistrationFieldException("Role is required for new Google registration");
        }
        if (!StringUtils.hasText(request.getUsername())) {
            throw new MissingGoogleRegistrationFieldException("Username is required for new Google registration");
        }
        if (request.getRole() == Role.ADMIN) {
            throw new InvalidRoleRegistrationException("Cannot self-register as ADMIN");
        }
    }

    private User buildAndPopulateNewGoogleUser(GoogleLoginRequest request, GoogleUserInfo googleUserInfo) {
        UserCreationContext context = UserCreationContext.builder()
                .certificationNumber(request.getCertificationNumber())
                .build();
        User newUser = userRegistrationFactory.create(request.getRole(), context);
        newUser.setEmail(googleUserInfo.getEmail());
        newUser.setName(googleUserInfo.getName() != null ? googleUserInfo.getName() : googleUserInfo.getEmail());
        newUser.setUsername(request.getUsername());
        newUser.setPassword(null);
        newUser.setRole(request.getRole());
        newUser.setGoogleSub(googleUserInfo.getGoogleSub());
        return newUser;
    }

    public ValidateTokenResponse validateToken(String token) {
        return tokenService.validate(token);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenValue) {
        return tokenService.refresh(refreshTokenValue);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        tokenService.revoke(refreshTokenValue);
    }

    @Transactional
    public void linkGoogle(String userId, String idToken) {
        GoogleUserInfo googleUserInfo = googleTokenVerifierService.verifyToken(idToken);

        userRepository.findByGoogleSub(googleUserInfo.getGoogleSub())
                .ifPresent(existingUser -> {
                    if (!existingUser.getId().equals(userId)) {
                        throw new GoogleSubAlreadyLinkedException("This Google account is already linked to another user");
                    }
                });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setGoogleSub(googleUserInfo.getGoogleSub());
        userRepository.save(user);
    }

    @Transactional
    public void setPassword(String userId, String rawPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
    }
}

package com.mysawit.identity.service;

import com.mysawit.identity.dto.AuthResponse;
import com.mysawit.identity.dto.GoogleLoginRequest;
import com.mysawit.identity.dto.GoogleUserInfo;
import com.mysawit.identity.enums.Role;
import com.mysawit.identity.event.UserRegisteredEvent;
import com.mysawit.identity.exception.DuplicateCertificationNumberException;
import com.mysawit.identity.exception.InvalidRoleRegistrationException;
import com.mysawit.identity.exception.MissingGoogleRegistrationFieldException;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.UserRepository;
import com.mysawit.identity.service.registration.UserCreationContext;
import com.mysawit.identity.service.registration.UserRegistrationFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRegistrationFactory userRegistrationFactory;
    private final TokenService tokenService;

    public GoogleAuthService(
            UserRepository userRepository,
            GoogleTokenVerifierService googleTokenVerifierService,
            ApplicationEventPublisher eventPublisher,
            UserRegistrationFactory userRegistrationFactory,
            TokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.eventPublisher = eventPublisher;
        this.userRegistrationFactory = userRegistrationFactory;
        this.tokenService = tokenService;
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
}

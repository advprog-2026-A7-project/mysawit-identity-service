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
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.UserRepository;
import com.mysawit.identity.service.registration.UserCreationContext;
import com.mysawit.identity.service.registration.UserRegistrationFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocalAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRegistrationFactory userRegistrationFactory;
    private final TokenService tokenService;

    public LocalAuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher,
            UserRegistrationFactory userRegistrationFactory,
            TokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return tokenService.issueTokens(user);
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
                user.getId(), user.getEmail(), user.getRole().name(), user.getUsername()));
    }
}

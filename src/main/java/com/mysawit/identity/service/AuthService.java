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
import com.mysawit.identity.exception.InvalidMandorException;
import com.mysawit.identity.exception.InvalidRoleRegistrationException;
import com.mysawit.identity.exception.InvalidTokenException;
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
import com.mysawit.identity.event.UserRegisteredEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final MandorRepository mandorRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final ApplicationEventPublisher eventPublisher;

    public AuthService(
            UserRepository userRepository,
            MandorRepository mandorRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            GoogleTokenVerifierService googleTokenVerifierService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.mandorRepository = mandorRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already exists");
        }

        Role role = request.getRole() != null ? request.getRole() : Role.BURUH;
        if (role == Role.ADMIN) {
            throw new InvalidRoleRegistrationException("Cannot self-register as ADMIN");
        }

        User user = buildUserByRole(request, role);
        user.setName(request.getUsername());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            if (role == Role.MANDOR) {
                throw new DuplicateCertificationNumberException("Certification number already exists");
            }
            throw e;
        }

        eventPublisher.publishEvent(new UserRegisteredEvent(
                savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name()));

        return buildAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        GoogleUserInfo googleUserInfo = googleTokenVerifierService.verifyToken(request.getIdToken());

        boolean[] isNewUser = {false};

        User user = userRepository.findByGoogleSub(googleUserInfo.getGoogleSub())
                .orElseGet(() -> userRepository.findByEmail(googleUserInfo.getEmail())
                        .map(existingUser -> {
                            existingUser.setGoogleSub(googleUserInfo.getGoogleSub());
                            return userRepository.save(existingUser);
                        })
                        .orElseGet(() -> {
                            isNewUser[0] = true;
                            Buruh newUser = new Buruh();
                            newUser.setEmail(googleUserInfo.getEmail());
                            newUser.setName(googleUserInfo.getName() != null ? googleUserInfo.getName() : googleUserInfo.getEmail());
                            newUser.setUsername(googleUserInfo.getEmail());
                            // Google-only accounts start without a local password.
                            newUser.setPassword(null);
                            newUser.setRole(Role.BURUH);
                            newUser.setGoogleSub(googleUserInfo.getGoogleSub());
                            return userRepository.save(newUser);
                        })
                );

        if (isNewUser[0]) {
            eventPublisher.publishEvent(new UserRegisteredEvent(
                    user.getId(), user.getEmail(), user.getRole().name()));
        }

        return buildAuthResponse(user);
    }

    public ValidateTokenResponse validateToken(String token) {
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
    public AuthResponse refreshToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RefreshTokenExpiredException("Refresh token has expired");
        }

        refreshTokenRepository.delete(refreshToken);

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new InvalidTokenException("User not found for refresh token"));

        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(refreshTokenRepository::delete);
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

    private AuthResponse buildAuthResponse(User user) {
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

    private User buildUserByRole(RegisterRequest request, Role role) {
        return switch (role) {
            case BURUH -> {
                Buruh buruh = new Buruh();
                String mandorId = trimToNull(request.getMandorId());
                if (mandorId != null) {
                    Mandor mandor = mandorRepository.findById(mandorId)
                            .orElseThrow(() -> new InvalidMandorException("Invalid mandorId"));
                    buruh.setMandor(mandor);
                }
                yield buruh;
            }
            case MANDOR -> {
                String certificationNumber = trimToNull(request.getCertificationNumber());
                if (certificationNumber == null) {
                    throw new MissingMandorCertificationException("Certification number is required for MANDOR");
                }
                if (mandorRepository.existsByCertificationNumber(certificationNumber)) {
                    throw new DuplicateCertificationNumberException("Certification number already exists");
                }

                Mandor mandor = new Mandor();
                mandor.setCertificationNumber(certificationNumber);
                yield mandor;
            }
            case SUPIR -> {
                Supir supir = new Supir();
                supir.setKebunId(trimToNull(request.getKebunId()));
                yield supir;
            }
            case ADMIN -> throw new InvalidRoleRegistrationException("Cannot self-register as ADMIN");
        };
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}

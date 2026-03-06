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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final MandorRepository mandorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    
    public AuthService(
            UserRepository userRepository,
            MandorRepository mandorRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            GoogleTokenVerifierService googleTokenVerifierService
    ) {
        this.userRepository = userRepository;
        this.mandorRepository = mandorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.googleTokenVerifierService = googleTokenVerifierService;
    }
    
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
        
        String token = jwtTokenProvider.generateToken(
            savedUser.getId(), 
            savedUser.getRole()
        );
        
        return new AuthResponse(
            token, 
            savedUser.getId(), 
            savedUser.getName(), 
            savedUser.getEmail(), 
            savedUser.getRole().name()
        );
    }
    
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        
        String token = jwtTokenProvider.generateToken(
            user.getId(), 
            user.getRole()
        );
        
        return new AuthResponse(
            token, 
            user.getId(), 
            user.getName(), 
            user.getEmail(), 
            user.getRole().name()
        );
    }

    public AuthResponse googleLogin(GoogleLoginRequest request) {
        GoogleUserInfo googleUserInfo = googleTokenVerifierService.verifyToken(request.getIdToken());
        
        User user = userRepository.findByGoogleSub(googleUserInfo.getGoogleSub())
                .orElseGet(() -> userRepository.findByEmail(googleUserInfo.getEmail())
                        .map(existingUser -> {
                            existingUser.setGoogleSub(googleUserInfo.getGoogleSub());
                            return userRepository.save(existingUser);
                        })
                        .orElseGet(() -> {
                            Buruh newUser = new Buruh();
                            newUser.setEmail(googleUserInfo.getEmail());
                            newUser.setName(googleUserInfo.getName() != null ? googleUserInfo.getName() : googleUserInfo.getEmail());
                            newUser.setUsername(googleUserInfo.getEmail());
                            newUser.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
                            newUser.setRole(Role.BURUH);
                            newUser.setGoogleSub(googleUserInfo.getGoogleSub());
                            return userRepository.save(newUser);
                        })
                );
        
        String token = jwtTokenProvider.generateToken(
            user.getId(), 
            user.getRole()
        );
        
        return new AuthResponse(
            token, 
            user.getId(), 
            user.getName(), 
            user.getEmail(), 
            user.getRole().name()
        );
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

package com.mysawit.identity.service;

import com.mysawit.identity.dto.GoogleUserInfo;
import com.mysawit.identity.exception.GoogleSubAlreadyLinkedException;
import com.mysawit.identity.exception.UserNotFoundException;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountLinkingService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenVerifierService googleTokenVerifierService;

    public AccountLinkingService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            GoogleTokenVerifierService googleTokenVerifierService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.googleTokenVerifierService = googleTokenVerifierService;
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

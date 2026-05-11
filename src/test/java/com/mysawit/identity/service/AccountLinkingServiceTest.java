package com.mysawit.identity.service;

import com.mysawit.identity.dto.GoogleUserInfo;
import com.mysawit.identity.exception.GoogleSubAlreadyLinkedException;
import com.mysawit.identity.exception.UserNotFoundException;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountLinkingServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private GoogleTokenVerifierService googleTokenVerifierService;
    private AccountLinkingService accountLinkingService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        googleTokenVerifierService = mock(GoogleTokenVerifierService.class);

        accountLinkingService = new AccountLinkingService(
                userRepository,
                passwordEncoder,
                googleTokenVerifierService
        );
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

        accountLinkingService.linkGoogle("user-1", "id-token");

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

        assertThrows(GoogleSubAlreadyLinkedException.class, () -> accountLinkingService.linkGoogle("user-1", "id-token"));
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

        accountLinkingService.linkGoogle("user-1", "id-token");

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

        assertThrows(UserNotFoundException.class, () -> accountLinkingService.linkGoogle("user-1", "id-token"));
    }

    @Test
    void setPasswordEncodesAndSaves() {
        User user = new User();
        user.setId("user-1");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("encoded-new");

        accountLinkingService.setPassword("user-1", "newpass");

        assertEquals("encoded-new", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void setPasswordThrowsWhenUserNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> accountLinkingService.setPassword("missing", "newpass"));
    }
}

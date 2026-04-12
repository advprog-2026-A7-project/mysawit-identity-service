package com.mysawit.identity.service;

import com.mysawit.identity.dto.InternalUserDetailResponse;
import com.mysawit.identity.enums.Role;
import com.mysawit.identity.exception.UserNotFoundException;
import com.mysawit.identity.model.Buruh;
import com.mysawit.identity.model.Mandor;
import com.mysawit.identity.model.Supir;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InternalUserServiceTest {

    private UserRepository userRepository;
    private InternalUserService internalUserService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        internalUserService = new InternalUserService(userRepository);
    }

    @Test
    void getUserByIdReturnsBasicUser() {
        User user = new User();
        user.setId("1");
        user.setName("User");
        user.setEmail("user@mail.com");
        user.setRole(Role.ADMIN);

        when(userRepository.findById("1")).thenReturn(Optional.of(user));

        InternalUserDetailResponse result = internalUserService.getUserById("1");

        assertEquals("1", result.getId());
        assertEquals("User", result.getName());
        assertEquals("user@mail.com", result.getEmail());
        assertEquals("ADMIN", result.getRole());
        assertNull(result.getMandorId());
    }

    @Test
    void getUserByIdReturnsBuruhWithMandor() {
        Mandor mandor = new Mandor();
        mandor.setId("mandor-1");
        mandor.setName("Mandor Name");

        Buruh buruh = new Buruh();
        buruh.setId("1");
        buruh.setName("Buruh");
        buruh.setEmail("buruh@mail.com");
        buruh.setRole(Role.BURUH);
        buruh.setMandor(mandor);

        when(userRepository.findById("1")).thenReturn(Optional.of(buruh));

        InternalUserDetailResponse result = internalUserService.getUserById("1");

        assertEquals("mandor-1", result.getMandorId());
        assertEquals("Mandor Name", result.getMandorName());
    }

    @Test
    void getUserByIdReturnsBuruhWithoutMandor() {
        Buruh buruh = new Buruh();
        buruh.setId("1");
        buruh.setName("Buruh");
        buruh.setEmail("buruh@mail.com");
        buruh.setRole(Role.BURUH);

        when(userRepository.findById("1")).thenReturn(Optional.of(buruh));

        InternalUserDetailResponse result = internalUserService.getUserById("1");

        assertNull(result.getMandorId());
        assertNull(result.getMandorName());
    }

    @Test
    void getUserByIdReturnsMandor() {
        Mandor mandor = new Mandor();
        mandor.setId("1");
        mandor.setName("Mandor");
        mandor.setEmail("mandor@mail.com");
        mandor.setRole(Role.MANDOR);
        mandor.setCertificationNumber("CERT-001");

        when(userRepository.findById("1")).thenReturn(Optional.of(mandor));

        InternalUserDetailResponse result = internalUserService.getUserById("1");

        assertEquals("CERT-001", result.getCertificationNumber());
    }

    @Test
    void getUserByIdReturnsSupir() {
        Supir supir = new Supir();
        supir.setId("1");
        supir.setName("Supir");
        supir.setEmail("supir@mail.com");
        supir.setRole(Role.SUPIR);
        supir.setKebunId("kebun-1");

        when(userRepository.findById("1")).thenReturn(Optional.of(supir));

        InternalUserDetailResponse result = internalUserService.getUserById("1");

        assertEquals("kebun-1", result.getKebunId());
    }

    @Test
    void getUserByIdThrowsWhenNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> internalUserService.getUserById("missing"));
    }
}

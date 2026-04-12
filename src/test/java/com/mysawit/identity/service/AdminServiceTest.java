package com.mysawit.identity.service;

import com.mysawit.identity.dto.UserDetailResponse;
import com.mysawit.identity.enums.Role;
import com.mysawit.identity.exception.CannotDeleteAdminUtamaException;
import com.mysawit.identity.exception.CannotDeleteSelfException;
import com.mysawit.identity.exception.InvalidMandorException;
import com.mysawit.identity.exception.InvalidUserRoleException;
import com.mysawit.identity.exception.UserNotFoundException;
import com.mysawit.identity.model.Buruh;
import com.mysawit.identity.model.Mandor;
import com.mysawit.identity.model.Supir;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.MandorRepository;
import com.mysawit.identity.repository.RefreshTokenRepository;
import com.mysawit.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminServiceTest {

    private UserRepository userRepository;
    private MandorRepository mandorRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        mandorRepository = mock(MandorRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        adminService = new AdminService(userRepository, mandorRepository, refreshTokenRepository);
        ReflectionTestUtils.setField(adminService, "adminUtamaEmail", "admin@mysawit.com");
    }

    @Test
    @SuppressWarnings("unchecked")
    void listUsersReturnsAllUsers() {
        User user = new User();
        user.setId("1");
        user.setUsername("user");
        user.setEmail("user@mail.com");
        user.setName("User");
        user.setRole(Role.BURUH);

        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(user));

        List<UserDetailResponse> result = adminService.listUsers(null, null, null);

        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listUsersReturnsBuruhWithMandorId() {
        Mandor mandor = new Mandor();
        mandor.setId("mandor-1");
        mandor.setName("Mandor");
        mandor.setRole(Role.MANDOR);

        Buruh buruh = new Buruh();
        buruh.setId("1");
        buruh.setUsername("buruh");
        buruh.setEmail("buruh@mail.com");
        buruh.setName("Buruh");
        buruh.setRole(Role.BURUH);
        buruh.setMandor(mandor);

        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(buruh));

        List<UserDetailResponse> result = adminService.listUsers(null, null, null);

        assertEquals("mandor-1", result.get(0).getMandorId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listUsersReturnsMandorWithCertification() {
        Mandor mandor = new Mandor();
        mandor.setId("1");
        mandor.setUsername("mandor");
        mandor.setEmail("mandor@mail.com");
        mandor.setName("Mandor");
        mandor.setRole(Role.MANDOR);
        mandor.setCertificationNumber("CERT-001");

        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(mandor));

        List<UserDetailResponse> result = adminService.listUsers(null, null, null);

        assertEquals("CERT-001", result.get(0).getCertificationNumber());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listUsersReturnsSupirWithKebunId() {
        Supir supir = new Supir();
        supir.setId("1");
        supir.setUsername("supir");
        supir.setEmail("supir@mail.com");
        supir.setName("Supir");
        supir.setRole(Role.SUPIR);
        supir.setKebunId("kebun-1");

        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(supir));

        List<UserDetailResponse> result = adminService.listUsers(null, null, null);

        assertEquals("kebun-1", result.get(0).getKebunId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listUsersReturnsBuruhWithoutMandor() {
        Buruh buruh = new Buruh();
        buruh.setId("1");
        buruh.setUsername("buruh");
        buruh.setEmail("buruh@mail.com");
        buruh.setName("Buruh");
        buruh.setRole(Role.BURUH);
        buruh.setMandor(null);

        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(buruh));

        List<UserDetailResponse> result = adminService.listUsers(null, null, null);

        assertNull(result.get(0).getMandorId());
    }

    @Test
    void getUserReturnsUser() {
        User user = new User();
        user.setId("1");
        user.setUsername("user");
        user.setEmail("user@mail.com");
        user.setName("User");
        user.setRole(Role.BURUH);

        when(userRepository.findById("1")).thenReturn(Optional.of(user));

        UserDetailResponse result = adminService.getUser("1");

        assertEquals("1", result.getId());
    }

    @Test
    void getUserThrowsWhenNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> adminService.getUser("missing"));
    }

    @Test
    void assignBuruhToMandorSucceeds() {
        Buruh buruh = new Buruh();
        buruh.setId("buruh-1");
        buruh.setRole(Role.BURUH);

        Mandor mandor = new Mandor();
        mandor.setId("mandor-1");

        when(userRepository.findById("buruh-1")).thenReturn(Optional.of(buruh));
        when(mandorRepository.findById("mandor-1")).thenReturn(Optional.of(mandor));

        adminService.assignBuruhToMandor("buruh-1", "mandor-1");

        assertEquals(mandor, buruh.getMandor());
        verify(userRepository).save(buruh);
    }

    @Test
    void assignBuruhToMandorThrowsWhenUserNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> adminService.assignBuruhToMandor("missing", "mandor-1"));
    }

    @Test
    void assignBuruhToMandorThrowsWhenNotBuruh() {
        User user = new User();
        user.setId("1");
        user.setRole(Role.SUPIR);

        when(userRepository.findById("1")).thenReturn(Optional.of(user));

        assertThrows(InvalidUserRoleException.class, () -> adminService.assignBuruhToMandor("1", "mandor-1"));
    }

    @Test
    void assignBuruhToMandorThrowsWhenMandorNotFound() {
        Buruh buruh = new Buruh();
        buruh.setId("buruh-1");
        buruh.setRole(Role.BURUH);

        when(userRepository.findById("buruh-1")).thenReturn(Optional.of(buruh));
        when(mandorRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(InvalidMandorException.class, () -> adminService.assignBuruhToMandor("buruh-1", "missing"));
    }

    @Test
    void unassignBuruhFromMandorSucceeds() {
        Mandor mandor = new Mandor();
        mandor.setId("mandor-1");

        Buruh buruh = new Buruh();
        buruh.setId("buruh-1");
        buruh.setRole(Role.BURUH);
        buruh.setMandor(mandor);

        when(userRepository.findById("buruh-1")).thenReturn(Optional.of(buruh));

        adminService.unassignBuruhFromMandor("buruh-1");

        assertNull(buruh.getMandor());
        verify(userRepository).save(buruh);
    }

    @Test
    void unassignBuruhFromMandorThrowsWhenUserNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> adminService.unassignBuruhFromMandor("missing"));
    }

    @Test
    void unassignBuruhFromMandorThrowsWhenNotBuruh() {
        User user = new User();
        user.setId("1");
        user.setRole(Role.MANDOR);

        when(userRepository.findById("1")).thenReturn(Optional.of(user));

        assertThrows(InvalidUserRoleException.class, () -> adminService.unassignBuruhFromMandor("1"));
    }

    @Test
    void deleteUserSucceeds() {
        User target = new User();
        target.setId("target-1");
        target.setEmail("user@mail.com");

        when(userRepository.findById("target-1")).thenReturn(Optional.of(target));

        adminService.deleteUser("admin-1", "target-1");

        verify(refreshTokenRepository).deleteByUserId("target-1");
        verify(userRepository).delete(target);
    }

    @Test
    void deleteUserThrowsWhenSelf() {
        assertThrows(CannotDeleteSelfException.class, () -> adminService.deleteUser("admin-1", "admin-1"));
    }

    @Test
    void deleteUserThrowsWhenNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> adminService.deleteUser("admin-1", "missing"));
    }

    @Test
    void deleteUserThrowsWhenAdminUtama() {
        User adminUtama = new User();
        adminUtama.setId("utama-1");
        adminUtama.setEmail("admin@mysawit.com");

        when(userRepository.findById("utama-1")).thenReturn(Optional.of(adminUtama));

        assertThrows(CannotDeleteAdminUtamaException.class, () -> adminService.deleteUser("admin-2", "utama-1"));
    }
}

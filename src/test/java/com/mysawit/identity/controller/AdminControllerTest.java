package com.mysawit.identity.controller;

import com.mysawit.identity.dto.AssignBuruhRequest;
import com.mysawit.identity.dto.MessageResponse;
import com.mysawit.identity.dto.UserDetailResponse;
import com.mysawit.identity.enums.Role;
import com.mysawit.identity.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminControllerTest {

    private AdminService adminService;
    private AdminController adminController;

    @BeforeEach
    void setUp() {
        adminService = mock(AdminService.class);
        adminController = new AdminController(adminService);
    }

    @Test
    void listUsersReturnsOk() {
        List<UserDetailResponse> users = List.of(UserDetailResponse.builder().id("1").build());
        when(adminService.listUsers("name", "email", Role.BURUH)).thenReturn(users);

        ResponseEntity<List<UserDetailResponse>> response = adminController.listUsers("name", "email", Role.BURUH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void listUsersWithNullParams() {
        when(adminService.listUsers(null, null, null)).thenReturn(List.of());

        ResponseEntity<List<UserDetailResponse>> response = adminController.listUsers(null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getUserReturnsOk() {
        UserDetailResponse user = UserDetailResponse.builder().id("1").build();
        when(adminService.getUser("1")).thenReturn(user);

        ResponseEntity<UserDetailResponse> response = adminController.getUser("1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("1", response.getBody().getId());
    }

    @Test
    void assignBuruhToMandorReturnsOk() {
        AssignBuruhRequest request = new AssignBuruhRequest("mandor-1");

        ResponseEntity<MessageResponse> response = adminController.assignBuruhToMandor("buruh-1", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Buruh assigned to Mandor successfully", response.getBody().getMessage());
        verify(adminService).assignBuruhToMandor("buruh-1", "mandor-1");
    }

    @Test
    void unassignBuruhFromMandorReturnsOk() {
        ResponseEntity<MessageResponse> response = adminController.unassignBuruhFromMandor("buruh-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Buruh unassigned from Mandor successfully", response.getBody().getMessage());
        verify(adminService).unassignBuruhFromMandor("buruh-1");
    }

    @Test
    void deleteUserReturnsOk() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin-1");

        ResponseEntity<MessageResponse> response = adminController.deleteUser(authentication, "target-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User deleted successfully", response.getBody().getMessage());
        verify(adminService).deleteUser("admin-1", "target-1");
    }
}

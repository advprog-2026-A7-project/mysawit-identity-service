package com.mysawit.identity.controller;

import com.mysawit.identity.dto.InternalUserDetailResponse;
import com.mysawit.identity.service.InternalUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InternalUserControllerTest {

    private InternalUserService internalUserService;
    private InternalUserController controller;

    @BeforeEach
    void setUp() {
        internalUserService = mock(InternalUserService.class);
        controller = new InternalUserController(internalUserService);
    }

    @Test
    void getUserByIdReturnsOk() {
        InternalUserDetailResponse expected = InternalUserDetailResponse.builder()
                .id("1")
                .name("User")
                .email("user@mail.com")
                .role("BURUH")
                .build();
        when(internalUserService.getUserById("1")).thenReturn(expected);

        ResponseEntity<InternalUserDetailResponse> response = controller.getUserById("1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
    }
}

package com.mysawit.identity.advice;

import com.mysawit.identity.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleDataIntegrityViolationReturnsConflictResponse() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/auth/register");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Data conflict", response.getBody().getMessage());
        assertEquals("/auth/register", response.getBody().getPath());
    }

    @Test
    void handleUnexpectedReturnsInternalServerErrorResponse() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/auth/login");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new RuntimeException("boom"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal server error", response.getBody().getMessage());
        assertEquals("/auth/login", response.getBody().getPath());
    }
}

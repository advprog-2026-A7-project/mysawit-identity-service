package com.mysawit.identity.advice;

import com.mysawit.identity.dto.ErrorResponse;
import com.mysawit.identity.exception.CannotDeleteAdminUtamaException;
import com.mysawit.identity.exception.CannotDeleteSelfException;
import com.mysawit.identity.exception.DuplicateEmailException;
import com.mysawit.identity.exception.GoogleSubAlreadyLinkedException;
import com.mysawit.identity.exception.InvalidCredentialsException;
import com.mysawit.identity.exception.InvalidMandorException;
import com.mysawit.identity.exception.InvalidUserRoleException;
import com.mysawit.identity.exception.RefreshTokenExpiredException;
import com.mysawit.identity.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void handleBadRequestForInvalidMandor() {
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(
                new InvalidMandorException("bad mandor"), request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad mandor", response.getBody().getMessage());
    }

    @Test
    void handleBadRequestForInvalidUserRole() {
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(
                new InvalidUserRoleException("not a buruh"), request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("not a buruh", response.getBody().getMessage());
    }

    @Test
    void handleValidationWithFieldError() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "field", "must not be blank");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(fieldError);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("must not be blank", response.getBody().getMessage());
    }

    @Test
    void handleValidationWithoutFieldError() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(null);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().getMessage());
    }

    @Test
    void handleValidationWithNullDefaultMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = mock(FieldError.class);
        when(fieldError.getDefaultMessage()).thenReturn(null);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(fieldError);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);
        assertEquals("Validation failed", response.getBody().getMessage());
    }

    @Test
    void handleUnauthorizedForInvalidCredentials() {
        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(
                new InvalidCredentialsException("bad creds"), request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void handleUnauthorizedForRefreshTokenExpired() {
        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(
                new RefreshTokenExpiredException("expired"), request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("expired", response.getBody().getMessage());
    }

    @Test
    void handleForbiddenActionForCannotDeleteSelf() {
        ResponseEntity<ErrorResponse> response = handler.handleForbiddenAction(
                new CannotDeleteSelfException("self"), request);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("self", response.getBody().getMessage());
    }

    @Test
    void handleForbiddenActionForAdminUtama() {
        ResponseEntity<ErrorResponse> response = handler.handleForbiddenAction(
                new CannotDeleteAdminUtamaException("admin utama"), request);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("admin utama", response.getBody().getMessage());
    }

    @Test
    void handleNotFound() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                new UserNotFoundException("not found"), request);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("not found", response.getBody().getMessage());
    }

    @Test
    void handleConflictForDuplicateEmail() {
        ResponseEntity<ErrorResponse> response = handler.handleConflict(
                new DuplicateEmailException("dup"), request);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void handleConflictForGoogleSubAlreadyLinked() {
        ResponseEntity<ErrorResponse> response = handler.handleConflict(
                new GoogleSubAlreadyLinkedException("linked"), request);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("linked", response.getBody().getMessage());
    }

    @Test
    void handleDataIntegrityViolationReturnsConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Data conflict", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    void handleForbiddenForAccessDenied() {
        ResponseEntity<ErrorResponse> response = handler.handleForbidden(
                new AccessDeniedException("denied"), request);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Access denied", response.getBody().getMessage());
    }

    @Test
    void handleForbiddenForAuthorizationDenied() {
        ResponseEntity<ErrorResponse> response = handler.handleForbidden(
                new AuthorizationDeniedException("denied"), request);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void handleUnexpectedReturnsInternalServerError() {
        Exception exception = new Exception("something broke");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(exception, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal server error", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
    }
}

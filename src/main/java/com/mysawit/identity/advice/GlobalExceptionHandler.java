package com.mysawit.identity.advice;

import com.mysawit.identity.dto.ErrorResponse;
import com.mysawit.identity.exception.CannotDeleteAdminUtamaException;
import com.mysawit.identity.exception.CannotDeleteSelfException;
import com.mysawit.identity.exception.DuplicateCertificationNumberException;
import com.mysawit.identity.exception.DuplicateEmailException;
import com.mysawit.identity.exception.GoogleSubAlreadyLinkedException;
import com.mysawit.identity.exception.InvalidCredentialsException;
import com.mysawit.identity.exception.InvalidMandorException;
import com.mysawit.identity.exception.InvalidRoleRegistrationException;
import com.mysawit.identity.exception.InvalidTokenException;
import com.mysawit.identity.exception.InvalidUserRoleException;
import com.mysawit.identity.exception.MissingGoogleRegistrationFieldException;
import com.mysawit.identity.exception.MissingMandorCertificationException;
import com.mysawit.identity.exception.RefreshTokenExpiredException;
import com.mysawit.identity.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            InvalidMandorException.class,
            InvalidRoleRegistrationException.class,
            MissingMandorCertificationException.class,
            MissingGoogleRegistrationFieldException.class,
            InvalidUserRoleException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException exception, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = "Validation failed";
        FieldError fieldError = exception.getBindingResult().getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            message = fieldError.getDefaultMessage();
        }

        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler({
            InvalidCredentialsException.class,
            InvalidTokenException.class,
            RefreshTokenExpiredException.class
    })
    public ResponseEntity<ErrorResponse> handleUnauthorized(RuntimeException exception, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({
            CannotDeleteSelfException.class,
            CannotDeleteAdminUtamaException.class
    })
    public ResponseEntity<ErrorResponse> handleForbiddenAction(RuntimeException exception, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException exception, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({
            DuplicateEmailException.class,
            DuplicateCertificationNumberException.class,
            GoogleSubAlreadyLinkedException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException exception, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Data conflict", request.getRequestURI());
    }

    @ExceptionHandler({
            AccessDeniedException.class,
            AuthorizationDeniedException.class
    })
    public ResponseEntity<ErrorResponse> handleForbidden(RuntimeException exception, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message, String path) {
        ErrorResponse response = new ErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
        return ResponseEntity.status(status).body(response);
    }
}

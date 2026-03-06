package com.mysawit.identity.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.mysawit.identity.dto.GoogleUserInfo;
import com.mysawit.identity.exception.InvalidTokenException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleTokenVerifierServiceFullTest {

    @Test
    void initCreatesVerifierWithClientId() {
        GoogleTokenVerifierService service = new GoogleTokenVerifierService();
        ReflectionTestUtils.setField(service, "clientId", "test-client-id");

        service.init();

        Object verifier = ReflectionTestUtils.getField(service, "verifier");
        assertNotNull(verifier);
    }

    @Test
    void initLogsWarningWhenClientIdMissing() {
        GoogleTokenVerifierService service = new GoogleTokenVerifierService();
        ReflectionTestUtils.setField(service, "clientId", "");

        service.init();

        Object verifier = ReflectionTestUtils.getField(service, "verifier");
        assertNotNull(verifier);
    }

    @Test
    void verifyTokenReturnsGoogleUserInfoOnSuccess() throws Exception {
        GoogleTokenVerifierService service = new GoogleTokenVerifierService();
        GoogleIdTokenVerifier mockVerifier = mock(GoogleIdTokenVerifier.class);
        ReflectionTestUtils.setField(service, "verifier", mockVerifier);

        GoogleIdToken mockIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-sub-123");
        payload.setEmail("user@google.com");
        payload.set("name", "Google User");

        when(mockVerifier.verify("valid-token")).thenReturn(mockIdToken);
        when(mockIdToken.getPayload()).thenReturn(payload);

        GoogleUserInfo result = service.verifyToken("valid-token");

        assertEquals("google-sub-123", result.getGoogleSub());
        assertEquals("user@google.com", result.getEmail());
        assertEquals("Google User", result.getName());
    }

    @Test
    void verifyTokenThrowsWhenIdTokenIsNull() throws Exception {
        GoogleTokenVerifierService service = new GoogleTokenVerifierService();
        GoogleIdTokenVerifier mockVerifier = mock(GoogleIdTokenVerifier.class);
        ReflectionTestUtils.setField(service, "verifier", mockVerifier);

        when(mockVerifier.verify("bad-token")).thenReturn(null);

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> service.verifyToken("bad-token")
        );
        assertEquals("Invalid Google ID token", exception.getMessage());
    }

    @Test
    void verifyTokenThrowsWhenVerifierThrowsException() throws Exception {
        GoogleTokenVerifierService service = new GoogleTokenVerifierService();
        GoogleIdTokenVerifier mockVerifier = mock(GoogleIdTokenVerifier.class);
        ReflectionTestUtils.setField(service, "verifier", mockVerifier);

        when(mockVerifier.verify("error-token")).thenThrow(new RuntimeException("network error"));

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> service.verifyToken("error-token")
        );
        assertEquals("Invalid Google ID token", exception.getMessage());
    }
}

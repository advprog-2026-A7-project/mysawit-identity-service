package com.mysawit.identity.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.mysawit.identity.dto.GoogleUserInfo;
import com.mysawit.identity.exception.InvalidTokenException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleTokenVerifierServiceTest {

    @Test
    void testVerifyTokenThrowsExceptionForInvalidToken() {
        GoogleTokenVerifierService service = new GoogleTokenVerifierService();
        assertThrows(InvalidTokenException.class, () -> service.verifyToken("invalid-token"));
    }

    @Test
    void verifyTokenReturnsUserInfoWhenGoogleTokenIsValid() throws Exception {
        GoogleTokenVerifierService service = new GoogleTokenVerifierService();
        GoogleIdTokenVerifier verifier = mock(GoogleIdTokenVerifier.class);
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);

        when(verifier.verify("valid-token")).thenReturn(idToken);
        when(idToken.getPayload()).thenReturn(payload);
        when(payload.getSubject()).thenReturn("google-sub");
        when(payload.getEmail()).thenReturn("user@mail.com");
        when(payload.get("name")).thenReturn("Google User");

        ReflectionTestUtils.setField(service, "verifier", verifier);

        GoogleUserInfo response = service.verifyToken("valid-token");

        assertEquals("google-sub", response.getGoogleSub());
        assertEquals("user@mail.com", response.getEmail());
        assertEquals("Google User", response.getName());
    }

    @Test
    void verifyTokenThrowsWhenGoogleTokenIsNull() throws Exception {
        GoogleTokenVerifierService service = new GoogleTokenVerifierService();
        GoogleIdTokenVerifier verifier = mock(GoogleIdTokenVerifier.class);

        when(verifier.verify("null-token")).thenReturn(null);
        ReflectionTestUtils.setField(service, "verifier", verifier);

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> service.verifyToken("null-token")
        );

        assertEquals("Invalid Google ID token", exception.getMessage());
    }

    @Test
    void initBuildsVerifierWhenClientIdIsMissing() {
        GoogleTokenVerifierService service = new GoogleTokenVerifierService();
        ReflectionTestUtils.setField(service, "clientId", " ");

        service.init();

        assertNotNull(ReflectionTestUtils.getField(service, "verifier"));
    }
}

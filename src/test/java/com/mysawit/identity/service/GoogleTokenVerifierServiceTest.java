package com.mysawit.identity.service;

import com.mysawit.identity.dto.GoogleUserInfo;
import com.mysawit.identity.exception.InvalidTokenException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GoogleTokenVerifierServiceTest {

    @Test
    void testVerifyTokenThrowsExceptionForInvalidToken() {
        GoogleTokenVerifierService service = new GoogleTokenVerifierService();
        assertThrows(InvalidTokenException.class, () -> service.verifyToken("invalid-token"));
    }
}

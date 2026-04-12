package com.mysawit.identity.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LinkGoogleRequestTest {

    @Test
    void noArgsConstructorAndSettersWork() {
        LinkGoogleRequest request = new LinkGoogleRequest();
        request.setIdToken("google-token");

        assertEquals("google-token", request.getIdToken());
    }

    @Test
    void allArgsConstructorWorks() {
        LinkGoogleRequest request = new LinkGoogleRequest("google-token");

        assertEquals("google-token", request.getIdToken());
    }
}

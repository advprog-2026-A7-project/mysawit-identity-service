package com.mysawit.identity.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SetPasswordRequestTest {

    @Test
    void noArgsConstructorAndSettersWork() {
        SetPasswordRequest request = new SetPasswordRequest();
        request.setPassword("newpass123");

        assertEquals("newpass123", request.getPassword());
    }

    @Test
    void allArgsConstructorWorks() {
        SetPasswordRequest request = new SetPasswordRequest("newpass123");

        assertEquals("newpass123", request.getPassword());
    }
}

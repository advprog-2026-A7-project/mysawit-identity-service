package com.mysawit.identity.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageResponseTest {

    @Test
    void noArgsConstructorAndSettersWork() {
        MessageResponse response = new MessageResponse();
        response.setMessage("Success");

        assertEquals("Success", response.getMessage());
    }

    @Test
    void allArgsConstructorWorks() {
        MessageResponse response = new MessageResponse("Success");

        assertEquals("Success", response.getMessage());
    }
}

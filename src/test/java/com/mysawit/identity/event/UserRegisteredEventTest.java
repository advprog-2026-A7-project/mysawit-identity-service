package com.mysawit.identity.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserRegisteredEventTest {

    @Test
    void allArgsConstructorAndGettersWork() {
        UserRegisteredEvent event = new UserRegisteredEvent("user-1", "user@mail.com", "BURUH");

        assertEquals("user-1", event.getUserId());
        assertEquals("user@mail.com", event.getEmail());
        assertEquals("BURUH", event.getRole());
    }

    @Test
    void noArgsConstructorAndSettersWork() {
        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setUserId("user-1");
        event.setEmail("user@mail.com");
        event.setRole("MANDOR");

        assertEquals("user-1", event.getUserId());
        assertEquals("user@mail.com", event.getEmail());
        assertEquals("MANDOR", event.getRole());
    }
}

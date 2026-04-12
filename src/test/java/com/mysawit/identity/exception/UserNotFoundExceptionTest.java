package com.mysawit.identity.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserNotFoundExceptionTest {

    @Test
    void constructorSetsMessage() {
        UserNotFoundException ex = new UserNotFoundException("not found");
        assertEquals("not found", ex.getMessage());
    }
}

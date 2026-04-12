package com.mysawit.identity.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CannotDeleteSelfExceptionTest {

    @Test
    void constructorSetsMessage() {
        CannotDeleteSelfException ex = new CannotDeleteSelfException("self");
        assertEquals("self", ex.getMessage());
    }
}

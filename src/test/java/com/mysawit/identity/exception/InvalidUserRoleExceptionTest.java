package com.mysawit.identity.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidUserRoleExceptionTest {

    @Test
    void constructorSetsMessage() {
        InvalidUserRoleException ex = new InvalidUserRoleException("bad role");
        assertEquals("bad role", ex.getMessage());
    }
}

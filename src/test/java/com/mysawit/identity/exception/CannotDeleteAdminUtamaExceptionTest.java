package com.mysawit.identity.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CannotDeleteAdminUtamaExceptionTest {

    @Test
    void constructorSetsMessage() {
        CannotDeleteAdminUtamaException ex = new CannotDeleteAdminUtamaException("admin utama");
        assertEquals("admin utama", ex.getMessage());
    }
}

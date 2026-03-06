package com.mysawit.identity.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void containsReturnsTrueForValidRoles() {
        assertTrue(Role.contains("BURUH"));
        assertTrue(Role.contains("MANDOR"));
        assertTrue(Role.contains("SUPIR"));
        assertTrue(Role.contains("ADMIN"));
    }

    @Test
    void containsReturnsFalseForInvalidRole() {
        assertFalse(Role.contains("UNKNOWN"));
        assertFalse(Role.contains(""));
        assertFalse(Role.contains("buruh"));
    }
}

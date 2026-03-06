package com.mysawit.identity.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleTest {

    @Test
    void containsReturnsTrueForKnownRoles() {
        assertTrue(Role.contains("BURUH"));
        assertTrue(Role.contains("MANDOR"));
        assertTrue(Role.contains("SUPIR"));
        assertTrue(Role.contains("ADMIN"));
    }

    @Test
    void containsReturnsFalseForUnknownOrMalformedRole() {
        assertFalse(Role.contains("UNKNOWN"));
        assertFalse(Role.contains(""));
        assertFalse(Role.contains("buruh"));
        assertFalse(Role.contains(null));
    }
}

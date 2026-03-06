package com.mysawit.identity.enums;

import org.junit.jupiter.api.Test;

<<<<<<< HEAD
import static org.junit.jupiter.api.Assertions.*;
=======
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
>>>>>>> 2d7e6d1 (test(identity): close uncovered branches and hit 100% line coverage)

class RoleTest {

    @Test
<<<<<<< HEAD
    void containsReturnsTrueForValidRoles() {
        assertTrue(Role.contains("BURUH"));
        assertTrue(Role.contains("MANDOR"));
        assertTrue(Role.contains("SUPIR"));
=======
    void containsReturnsTrueForKnownRoleName() {
>>>>>>> 2d7e6d1 (test(identity): close uncovered branches and hit 100% line coverage)
        assertTrue(Role.contains("ADMIN"));
    }

    @Test
<<<<<<< HEAD
    void containsReturnsFalseForInvalidRole() {
        assertFalse(Role.contains("UNKNOWN"));
        assertFalse(Role.contains(""));
        assertFalse(Role.contains("buruh"));
=======
    void containsReturnsFalseForUnknownOrNullRoleName() {
        assertFalse(Role.contains("UNKNOWN"));
        assertFalse(Role.contains(null));
>>>>>>> 2d7e6d1 (test(identity): close uncovered branches and hit 100% line coverage)
    }
}

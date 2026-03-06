package com.mysawit.identity.dto;

import org.junit.jupiter.api.Test;

<<<<<<< HEAD
import static org.junit.jupiter.api.Assertions.*;
=======
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
>>>>>>> 2d7e6d1 (test(identity): close uncovered branches and hit 100% line coverage)

class ValidateTokenResponseTest {

    @Test
<<<<<<< HEAD
    void noArgConstructorCreatesInstance() {
=======
    void noArgsConstructorAndSettersWork() {
>>>>>>> 2d7e6d1 (test(identity): close uncovered branches and hit 100% line coverage)
        ValidateTokenResponse response = new ValidateTokenResponse();

        assertFalse(response.isValid());
        assertNull(response.getUsername());
<<<<<<< HEAD
    }

    @Test
    void settersUpdateFields() {
        ValidateTokenResponse response = new ValidateTokenResponse();
=======

>>>>>>> 2d7e6d1 (test(identity): close uncovered branches and hit 100% line coverage)
        response.setValid(true);
        response.setUsername("user@mail.com");

        assertTrue(response.isValid());
        assertEquals("user@mail.com", response.getUsername());
    }
<<<<<<< HEAD
=======

    @Test
    void allArgsConstructorSetsFields() {
        ValidateTokenResponse response = new ValidateTokenResponse(true, "admin@mail.com");

        assertTrue(response.isValid());
        assertEquals("admin@mail.com", response.getUsername());
    }
>>>>>>> 2d7e6d1 (test(identity): close uncovered branches and hit 100% line coverage)
}

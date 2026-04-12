package com.mysawit.identity.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoogleSubAlreadyLinkedExceptionTest {

    @Test
    void constructorSetsMessage() {
        GoogleSubAlreadyLinkedException ex = new GoogleSubAlreadyLinkedException("linked");
        assertEquals("linked", ex.getMessage());
    }
}

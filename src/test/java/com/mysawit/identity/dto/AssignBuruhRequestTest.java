package com.mysawit.identity.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssignBuruhRequestTest {

    @Test
    void noArgsConstructorAndSettersWork() {
        AssignBuruhRequest request = new AssignBuruhRequest();
        request.setMandorId("mandor-1");

        assertEquals("mandor-1", request.getMandorId());
    }

    @Test
    void allArgsConstructorWorks() {
        AssignBuruhRequest request = new AssignBuruhRequest("mandor-1");

        assertEquals("mandor-1", request.getMandorId());
    }
}

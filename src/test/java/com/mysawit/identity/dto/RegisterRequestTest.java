package com.mysawit.identity.dto;

import com.mysawit.identity.enums.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestTest {

    @Test
    void gettersAndSettersWork() {
        RegisterRequest request = new RegisterRequest();

        request.setUsername("user");
        request.setEmail("user@mail.com");
        request.setPassword("secret123");
        request.setRole(Role.MANDOR);
        request.setCertificationNumber("CERT-001");
        request.setMandorId("mandor-1");
        request.setKebunId("kebun-1");

        assertEquals("user", request.getUsername());
        assertEquals("user@mail.com", request.getEmail());
        assertEquals("secret123", request.getPassword());
        assertEquals(Role.MANDOR, request.getRole());
        assertEquals("CERT-001", request.getCertificationNumber());
        assertEquals("mandor-1", request.getMandorId());
        assertEquals("kebun-1", request.getKebunId());
    }
}

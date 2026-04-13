package com.mysawit.identity.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InternalUserDetailResponseTest {

    @Test
    void builderWorks() {
        InternalUserDetailResponse response = InternalUserDetailResponse.builder()
                .id("1")
                .name("User Name")
                .email("user@mail.com")
                .role("BURUH")
                .mandorId("mandor-1")
                .mandorName("Mandor Name")
                .certificationNumber("CERT-001")
                .kebunId("kebun-1")
                .build();

        assertEquals("1", response.getId());
        assertEquals("User Name", response.getName());
        assertEquals("user@mail.com", response.getEmail());
        assertEquals("BURUH", response.getRole());
        assertEquals("mandor-1", response.getMandorId());
        assertEquals("Mandor Name", response.getMandorName());
        assertEquals("CERT-001", response.getCertificationNumber());
        assertEquals("kebun-1", response.getKebunId());
    }

    @Test
    void noArgsConstructorAndSettersWork() {
        InternalUserDetailResponse response = new InternalUserDetailResponse();
        response.setId("1");
        response.setName("name");

        assertEquals("1", response.getId());
        assertEquals("name", response.getName());
    }

    @Test
    void allArgsConstructorWorks() {
        InternalUserDetailResponse response = new InternalUserDetailResponse("1", "name", "email", "BURUH", "m1", "mName", "CERT", "k1");

        assertEquals("1", response.getId());
        assertEquals("mName", response.getMandorName());
    }
}

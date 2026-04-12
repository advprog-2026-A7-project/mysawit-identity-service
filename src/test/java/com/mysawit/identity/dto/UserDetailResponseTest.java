package com.mysawit.identity.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserDetailResponseTest {

    @Test
    void builderWorks() {
        LocalDateTime now = LocalDateTime.now();
        UserDetailResponse response = UserDetailResponse.builder()
                .id("1")
                .username("user")
                .email("user@mail.com")
                .name("User Name")
                .role("BURUH")
                .createdAt(now)
                .mandorId("mandor-1")
                .certificationNumber("CERT-001")
                .kebunId("kebun-1")
                .build();

        assertEquals("1", response.getId());
        assertEquals("user", response.getUsername());
        assertEquals("user@mail.com", response.getEmail());
        assertEquals("User Name", response.getName());
        assertEquals("BURUH", response.getRole());
        assertEquals(now, response.getCreatedAt());
        assertEquals("mandor-1", response.getMandorId());
        assertEquals("CERT-001", response.getCertificationNumber());
        assertEquals("kebun-1", response.getKebunId());
    }

    @Test
    void noArgsConstructorAndSettersWork() {
        UserDetailResponse response = new UserDetailResponse();
        response.setId("1");
        response.setUsername("user");

        assertEquals("1", response.getId());
        assertEquals("user", response.getUsername());
    }

    @Test
    void allArgsConstructorWorks() {
        LocalDateTime now = LocalDateTime.now();
        UserDetailResponse response = new UserDetailResponse("1", "user", "email", "name", "BURUH", now, "m1", "CERT", "k1");

        assertEquals("1", response.getId());
        assertEquals("CERT", response.getCertificationNumber());
    }
}

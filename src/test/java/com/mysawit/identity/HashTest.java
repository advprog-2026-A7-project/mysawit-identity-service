package com.mysawit.identity;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HashTest {
    @Test
    void checkHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = "$2a$10$7EqJtq98hPqEX7fNZaFWoOHi6XoN1J0YyqS/fWznuaCG2l9VmOAXa";
        System.out.println("HASH MATCHES ADMIN123: " + encoder.matches("admin123", hash));
    }
}

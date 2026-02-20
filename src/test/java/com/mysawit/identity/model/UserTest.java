package com.mysawit.identity.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void gettersSettersAndDefaultRoleWork() {
        User user = new User();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 1, 10, 0);

        assertEquals("USER", user.getRole());

        user.setId(1L);
        user.setUsername("user");
        user.setEmail("user@mail.com");
        user.setPassword("secret");
        user.setRole("ADMIN");
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(updatedAt);

        assertEquals(1L, user.getId());
        assertEquals("user", user.getUsername());
        assertEquals("user@mail.com", user.getEmail());
        assertEquals("secret", user.getPassword());
        assertEquals("ADMIN", user.getRole());
        assertEquals(createdAt, user.getCreatedAt());
        assertEquals(updatedAt, user.getUpdatedAt());
    }

    @Test
    void lifecycleHooksSetTimestamps() {
        User user = new User();

        user.onCreate();

        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());

        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
        user.setUpdatedAt(beforeUpdate.minusDays(1));

        user.onUpdate();

        assertTrue(user.getUpdatedAt().isAfter(beforeUpdate));
    }
}

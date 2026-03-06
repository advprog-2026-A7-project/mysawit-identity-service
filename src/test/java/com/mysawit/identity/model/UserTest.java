package com.mysawit.identity.model;

import com.mysawit.identity.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void gettersSettersAndDefaultRoleWork() {
        User user = new User();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 1, 10, 0);

        assertNull(user.getRole());

        user.setId("1");
        user.setUsername("user");
        user.setEmail("user@mail.com");
        user.setPassword("secret");
        user.setRole(Role.ADMIN);
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(updatedAt);

        assertEquals("1", user.getId());
        assertEquals("user", user.getUsername());
        assertEquals("user@mail.com", user.getEmail());
        assertEquals("secret", user.getPassword());
        assertEquals(Role.ADMIN, user.getRole());
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

    @Test
    void getAuthoritiesReturnsRoleWhenSet() {
        User user = new User();
        user.setRole(Role.ADMIN);

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        assertEquals(1, authorities.size());
        assertEquals("ROLE_ADMIN", authorities.iterator().next().getAuthority());
    }

    @Test
    void getAuthoritiesReturnsEmptyListWhenRoleNull() {
        User user = new User();

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        assertTrue(authorities.isEmpty());
    }

    @Test
    void springSecurityAccountFlagsDefaultToTrue() {
        User user = new User();

        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }

    @Test
    void springSecurityAccountFlagsCanBeDisabled() {
        User user = new User();
        user.setAccountNonExpired(false);
        user.setAccountNonLocked(false);
        user.setCredentialsNonExpired(false);
        user.setEnabled(false);

        assertFalse(user.isAccountNonExpired());
        assertFalse(user.isAccountNonLocked());
        assertFalse(user.isCredentialsNonExpired());
        assertFalse(user.isEnabled());
    }
}

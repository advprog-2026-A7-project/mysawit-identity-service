package com.mysawit.identity.integration;

import com.mysawit.identity.enums.Role;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveUserPersistsEntity() {
        User user = buildUser("user1", "user1@mail.com");

        User saved = userRepository.save(user);
        entityManager.flush();

        User found = entityManager.find(User.class, saved.getId());
        assertNotNull(found);
        assertEquals("user1@mail.com", found.getEmail());
        assertEquals("user1", found.getUsername());
        assertEquals(Role.BURUH, found.getRole());
    }

    @Test
    void existsByEmailReturnsTrueWhenUserExists() {
        entityManager.persistAndFlush(buildUser("user2", "exists@mail.com"));

        assertTrue(userRepository.existsByEmail("exists@mail.com"));
        assertFalse(userRepository.existsByEmail("nope@mail.com"));
    }

    @Test
    void findByEmailReturnsUserWhenExists() {
        entityManager.persistAndFlush(buildUser("user3", "find@mail.com"));

        Optional<User> result = userRepository.findByEmail("find@mail.com");

        assertTrue(result.isPresent());
        assertEquals("find@mail.com", result.get().getEmail());
    }

    @Test
    void uniqueEmailConstraintThrowsException() {
        entityManager.persistAndFlush(buildUser("user4a", "dup@mail.com"));

        User dup = buildUser("user4b", "dup@mail.com");
        assertThrows(Exception.class, () -> {
            userRepository.saveAndFlush(dup);
        });
    }

    private User buildUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setName(username);
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setRole(Role.BURUH);
        return user;
    }
}

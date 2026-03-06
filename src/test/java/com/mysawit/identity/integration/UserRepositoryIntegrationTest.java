package com.mysawit.identity.integration;

import com.mysawit.identity.enums.Role;
import com.mysawit.identity.model.Buruh;
import com.mysawit.identity.model.Mandor;
import com.mysawit.identity.model.Supir;
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

    @Test
    void saveMandorPersistsUserAndMandorTables() {
        Mandor mandor = new Mandor();
        mandor.setUsername("mandor1");
        mandor.setName("mandor1");
        mandor.setEmail("mandor1@mail.com");
        mandor.setPassword("encoded");
        mandor.setRole(Role.MANDOR);
        mandor.setCertificationNumber("CERT-001");

        Mandor saved = userRepository.saveAndFlush(mandor);

        Number usersCount = (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT COUNT(*) FROM users WHERE id = :id")
                .setParameter("id", saved.getId())
                .getSingleResult();
        Number mandorsCount = (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT COUNT(*) FROM mandors WHERE id = :id")
                .setParameter("id", saved.getId())
                .getSingleResult();

        assertEquals(1L, usersCount.longValue());
        assertEquals(1L, mandorsCount.longValue());
    }

    @Test
    void saveSupirPersistsUserAndSupirTables() {
        Supir supir = new Supir();
        supir.setUsername("supir1");
        supir.setName("supir1");
        supir.setEmail("supir1@mail.com");
        supir.setPassword("encoded");
        supir.setRole(Role.SUPIR);
        supir.setKebunId("kebun-001");

        Supir saved = userRepository.saveAndFlush(supir);

        Number usersCount = (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT COUNT(*) FROM users WHERE id = :id")
                .setParameter("id", saved.getId())
                .getSingleResult();
        Number supirsCount = (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT COUNT(*) FROM supirs WHERE id = :id")
                .setParameter("id", saved.getId())
                .getSingleResult();

        assertEquals(1L, usersCount.longValue());
        assertEquals(1L, supirsCount.longValue());
    }

    @Test
    void saveBuruhWithMandorPersistsRelation() {
        Mandor mandor = new Mandor();
        mandor.setUsername("mandor2");
        mandor.setName("mandor2");
        mandor.setEmail("mandor2@mail.com");
        mandor.setPassword("encoded");
        mandor.setRole(Role.MANDOR);
        mandor.setCertificationNumber("CERT-002");
        Mandor savedMandor = entityManager.persistFlushFind(mandor);

        Buruh buruh = new Buruh();
        buruh.setUsername("buruh1");
        buruh.setName("buruh1");
        buruh.setEmail("buruh1@mail.com");
        buruh.setPassword("encoded");
        buruh.setRole(Role.BURUH);
        buruh.setMandor(savedMandor);

        Buruh savedBuruh = userRepository.saveAndFlush(buruh);

        String mandorId = (String) entityManager.getEntityManager()
                .createNativeQuery("SELECT mandor_id FROM buruhs WHERE id = :id")
                .setParameter("id", savedBuruh.getId())
                .getSingleResult();

        assertEquals(savedMandor.getId(), mandorId);
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

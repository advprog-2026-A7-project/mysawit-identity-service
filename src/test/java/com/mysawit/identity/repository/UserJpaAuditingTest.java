package com.mysawit.identity.repository;

import com.mysawit.identity.enums.Role;
import com.mysawit.identity.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserJpaAuditingTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUpAuditor() {
        if (userRepository.findByEmail("system@audit.com").isEmpty()) {
            userRepository.saveAndFlush(User.builder()
                    .username("system_auditor")
                    .email("system@audit.com")
                    .name("System Auditor")
                    .password("x")
                    .role(Role.ADMIN)
                    .build());
        }
    }

    @TestConfiguration
    @EnableJpaAuditing(auditorAwareRef = "testAuditorProvider")
    static class TestAuditConfig {
        @Bean
        AuditorAware<User> testAuditorProvider(UserRepository userRepository) {
            return () -> userRepository.findByEmail("system@audit.com");
        }
    }

    @Test
    void shouldAutoPopulateAllAuditFieldsWithoutManualAssignment() {
        User user = User.builder()
                .username("audit_tester")
                .email("auditjpa@test.com")
                .name("JPA Audit Test User")
                .password("$2a$10$hashedPasswordPlaceholderXX")
                .role(Role.BURUH)
                .build();

        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getCreatedAt())
                .as("createdAt harus tidak null (saat ini via @PrePersist, target: @CreatedDate)")
                .isNotNull();

        assertThat(saved.getUpdatedAt())
                .as("updatedAt harus tidak null (saat ini via @PrePersist, target: @LastModifiedDate)")
                .isNotNull();

        assertThat(saved.getCreatedBy())
                .as("createdBy harus berisi User entity yang diisi oleh AuditorAware<User> via @CreatedBy — SAAT INI NULL (RED)")
                .isNotNull();

        assertThat(saved.getUpdatedBy())
                .as("updatedBy harus berisi User entity yang diisi oleh AuditorAware<User> via @LastModifiedBy — SAAT INI NULL (RED)")
                .isNotNull();
    }
}

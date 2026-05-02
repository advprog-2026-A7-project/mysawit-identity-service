package com.mysawit.identity.repository;

import com.mysawit.identity.enums.Role;
import com.mysawit.identity.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [RED] JPA Auditing — memverifikasi bahwa Spring Data JPA Auditing
 * (bukan @PrePersist manual) secara otomatis mengisi semua field audit.
 */
@DataJpaTest
@ActiveProfiles("test")
class UserJpaAuditingTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldAutoPopulateAllAuditFieldsWithoutManualAssignment() {
        // Arrange: buat User TANPA mengisi field audit secara manual
        User user = User.builder()
                .username("audit_tester")
                .email("auditjpa@test.com")
                .name("JPA Audit Test User")
                .password("$2a$10$hashedPasswordPlaceholderXX")
                .role(Role.BURUH)
                .build();

        // Act: simpan dan paksa flush ke database — Auditing listener harus berjalan
        User saved = userRepository.saveAndFlush(user);

        // Assert createdAt: saat ini LULUS via @PrePersist — target GREEN: diisi @CreatedDate
        assertThat(saved.getCreatedAt())
                .as("createdAt harus tidak null (saat ini via @PrePersist, target: @CreatedDate)")
                .isNotNull();

        // Assert updatedAt: saat ini LULUS via @PrePersist — target GREEN: diisi @LastModifiedDate
        assertThat(saved.getUpdatedAt())
                .as("updatedAt harus tidak null (saat ini via @PrePersist, target: @LastModifiedDate)")
                .isNotNull();

        // [RED] GAGAL: @EnableJpaAuditing belum dikonfigurasi + tidak ada @CreatedBy pada field
        // GREEN: AuditorAware<User> harus mengembalikan User entity yang sudah tersimpan,
        assertThat(saved.getCreatedBy())
                .as("createdBy harus berisi User entity yang diisi oleh AuditorAware<User> via @CreatedBy — SAAT INI NULL (RED)")
                .isNotNull();

        // [RED] GAGAL: @EnableJpaAuditing belum dikonfigurasi + tidak ada @LastModifiedBy pada field
        // GREEN: AuditorAware<User> yang sama harus mengembalikan User entity untuk @LastModifiedBy.
        assertThat(saved.getUpdatedBy())
                .as("updatedBy harus berisi User entity yang diisi oleh AuditorAware<User> via @LastModifiedBy — SAAT INI NULL (RED)")
                .isNotNull();
    }
}

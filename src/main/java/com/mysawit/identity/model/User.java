package com.mysawit.identity.model;

import com.mysawit.identity.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_username", columnList = "username"),
                @Index(name = "idx_user_email", columnList = "email")
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
public class User implements UserDetails {

    @Id
    @EqualsAndHashCode.Include
    @Column(nullable = false, updatable = false, unique = true, length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 150)
    private String name;

    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(unique = true)
    private String googleSub;

    // Account flags for Spring Security

    @Builder.Default
    @Column(nullable = false)
    private boolean accountNonExpired = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean accountNonLocked = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean credentialsNonExpired = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    // Audit fields

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }

        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Spring Security integration

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return Collections.emptyList();
        }

        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}

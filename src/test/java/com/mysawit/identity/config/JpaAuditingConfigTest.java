package com.mysawit.identity.config;

import com.mysawit.identity.enums.Role;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JpaAuditingConfigTest {

    private final JpaAuditingConfig config = new JpaAuditingConfig();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsEmptyWhenAuthenticationIsNull() {
        UserRepository repo = mock(UserRepository.class);
        AuditorAware<User> auditor = config.auditorProvider(repo);

        SecurityContextHolder.clearContext();

        assertThat(auditor.getCurrentAuditor()).isEmpty();
        verifyNoInteractions(repo);
    }

    @Test
    void returnsEmptyWhenNotAuthenticated() {
        UserRepository repo = mock(UserRepository.class);
        AuditorAware<User> auditor = config.auditorProvider(repo);

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(auditor.getCurrentAuditor()).isEmpty();
        verifyNoInteractions(repo);
    }

    @Test
    void returnsEmptyWhenPrincipalIsAnonymous() {
        UserRepository repo = mock(UserRepository.class);
        AuditorAware<User> auditor = config.auditorProvider(repo);

        Authentication auth = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(auditor.getCurrentAuditor()).isEmpty();
        verifyNoInteractions(repo);
    }

    @Test
    void returnsUserWhenAuthenticatedAndFoundInRepo() {
        UserRepository repo = mock(UserRepository.class);
        User u = User.builder()
                .id("user-123")
                .username("u")
                .email("u@u.com")
                .name("U")
                .password("p")
                .role(Role.ADMIN)
                .build();
        when(repo.findById("user-123")).thenReturn(Optional.of(u));

        AuditorAware<User> auditor = config.auditorProvider(repo);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user-123",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<User> result = auditor.getCurrentAuditor();
        assertThat(result).containsSame(u);
    }

    @Test
    void returnsEmptyWhenAuthenticatedButUserNotInRepo() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.findById("ghost")).thenReturn(Optional.empty());

        AuditorAware<User> auditor = config.auditorProvider(repo);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "ghost",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(auditor.getCurrentAuditor()).isEmpty();
    }
}

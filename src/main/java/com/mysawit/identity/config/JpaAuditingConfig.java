package com.mysawit.identity.config;

import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    private static final String ANONYMOUS_PRINCIPAL = "anonymousUser";

    @Bean
    public AuditorAware<User> auditorProvider(UserRepository userRepository) {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null
                    || !auth.isAuthenticated()
                    || ANONYMOUS_PRINCIPAL.equals(auth.getPrincipal())) {
                return Optional.empty();
            }
            return userRepository.findById(auth.getName());
        };
    }
}

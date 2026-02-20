package com.mysawit.identity.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SecurityConfigTest {

    @Test
    void filterChainConfiguresHttpSecurity() throws Exception {
        SecurityConfig securityConfig = new SecurityConfig();
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins", "http://localhost:3000,http://localhost:5173");

        HttpSecurity http = mock(HttpSecurity.class);
        DefaultSecurityFilterChain expectedFilterChain = mock(DefaultSecurityFilterChain.class);

        when(http.cors(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Customizer<CorsConfigurer<HttpSecurity>> customizer = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            CorsConfigurer<HttpSecurity> corsConfigurer = mock(CorsConfigurer.class);
            when(corsConfigurer.configurationSource(any())).thenReturn(corsConfigurer);
            customizer.customize(corsConfigurer);
            verify(corsConfigurer).configurationSource(any());
            return http;
        });

        when(http.csrf(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Customizer<CsrfConfigurer<HttpSecurity>> customizer = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            CsrfConfigurer<HttpSecurity> csrfConfigurer = mock(CsrfConfigurer.class);
            when(csrfConfigurer.disable()).thenReturn(http);
            customizer.customize(csrfConfigurer);
            verify(csrfConfigurer).disable();
            return http;
        });

        when(http.sessionManagement(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Customizer<SessionManagementConfigurer<HttpSecurity>> customizer = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            SessionManagementConfigurer<HttpSecurity> sessionManagementConfigurer = mock(SessionManagementConfigurer.class);
            when(sessionManagementConfigurer.sessionCreationPolicy(any())).thenReturn(sessionManagementConfigurer);
            customizer.customize(sessionManagementConfigurer);
            verify(sessionManagementConfigurer).sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            return http;
        });

        when(http.authorizeHttpRequests(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> customizer =
                    invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry =
                    mock(AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry.class);
            @SuppressWarnings("unchecked")
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl authorizedUrl =
                    mock(AuthorizeHttpRequestsConfigurer.AuthorizedUrl.class);

            when(registry.requestMatchers(anyString(), anyString())).thenReturn(authorizedUrl);
            when(authorizedUrl.permitAll()).thenReturn(registry);
            when(registry.anyRequest()).thenReturn(authorizedUrl);
            when(authorizedUrl.authenticated()).thenReturn(registry);

            customizer.customize(registry);

            verify(registry).requestMatchers("/api/auth/**", "/actuator/**");
            verify(authorizedUrl).permitAll();
            verify(registry).anyRequest();
            verify(authorizedUrl).authenticated();
            return http;
        });

        when(http.build()).thenReturn(expectedFilterChain);

        SecurityFilterChain result = securityConfig.filterChain(http);

        assertSame(expectedFilterChain, result);
    }

    @Test
    void corsConfigurationSourceBuildsExpectedConfiguration() {
        SecurityConfig securityConfig = new SecurityConfig();
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins", "http://localhost:3000,http://localhost:5173");

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        assertInstanceOf(UrlBasedCorsConfigurationSource.class, source);
        UrlBasedCorsConfigurationSource urlSource = (UrlBasedCorsConfigurationSource) source;
        CorsConfiguration configuration = urlSource.getCorsConfigurations().get("/**");

        assertNotNull(configuration);
        assertEquals(2, configuration.getAllowedOrigins().size());
        assertTrue(configuration.getAllowedMethods().contains("GET"));
        assertEquals(1, configuration.getAllowedHeaders().size());
        assertEquals("*", configuration.getAllowedHeaders().get(0));
        assertTrue(Boolean.TRUE.equals(configuration.getAllowCredentials()));
    }

    @Test
    void passwordEncoderHashesAndMatches() {
        SecurityConfig securityConfig = new SecurityConfig();

        PasswordEncoder encoder = securityConfig.passwordEncoder();

        String hashed = encoder.encode("secret123");
        assertNotEquals("secret123", hashed);
        assertTrue(encoder.matches("secret123", hashed));
    }
}

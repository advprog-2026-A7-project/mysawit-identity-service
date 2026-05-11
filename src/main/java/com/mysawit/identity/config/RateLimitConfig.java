package com.mysawit.identity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.identity.security.LoginRateLimitFilter;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    @Bean
    public FilterRegistrationBean<LoginRateLimitFilter> loginRateLimitFilter(
            RateLimitBucketStore store, ObjectMapper objectMapper) {

        FilterRegistrationBean<LoginRateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new LoginRateLimitFilter(store, objectMapper));
        registration.addUrlPatterns("/api/auth/login");
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER - 1);
        return registration;
    }
}

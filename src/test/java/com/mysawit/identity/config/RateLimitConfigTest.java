package com.mysawit.identity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.identity.security.LoginRateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitConfigTest {

    @Test
    void registersFilterForLoginUrlWithExpectedOrder() {
        RateLimitConfig config = new RateLimitConfig();
        RateLimitBucketStore store = new RateLimitBucketStore();
        ObjectMapper mapper = new ObjectMapper();

        FilterRegistrationBean<LoginRateLimitFilter> registration =
                config.loginRateLimitFilter(store, mapper);

        assertThat(registration).isNotNull();
        assertThat(registration.getFilter()).isInstanceOf(LoginRateLimitFilter.class);
        assertThat(registration.getUrlPatterns()).containsExactly("/api/auth/login");
        assertThat(registration.getOrder())
                .isEqualTo(SecurityProperties.DEFAULT_FILTER_ORDER - 1);
    }
}

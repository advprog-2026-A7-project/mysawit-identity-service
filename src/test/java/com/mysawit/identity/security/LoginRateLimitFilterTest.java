package com.mysawit.identity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mysawit.identity.config.RateLimitBucketStore;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LoginRateLimitFilterTest {

    private RateLimitBucketStore store;
    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        store = new RateLimitBucketStore();
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        filter = new LoginRateLimitFilter(store, mapper);
    }

    @Test
    void doFilterPassesRequestThroughWhenWithinLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilterReturns429WhenRateLimitExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.2");
        FilterChain passingChain = mock(FilterChain.class);

        for (int i = 0; i < 5; i++) {
            filter.doFilter(request, new MockHttpServletResponse(), passingChain);
        }
        verify(passingChain, times(5))
                .doFilter(ArgumentMatchers.any(), ArgumentMatchers.any());

        FilterChain blockedChain = mock(FilterChain.class);
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request, blockedResponse, blockedChain);

        verify(blockedChain, never())
                .doFilter(ArgumentMatchers.any(), ArgumentMatchers.any());
        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getContentType()).startsWith("application/json");
        assertThat(blockedResponse.getCharacterEncoding()).isEqualTo("UTF-8");

        String body = blockedResponse.getContentAsString();
        assertThat(body).contains("\"status\":429");
        assertThat(body).contains(LoginRateLimitFilter.RATE_LIMIT_MESSAGE);
        assertThat(body).contains("\"path\":\"/api/auth/login\"");
        assertThat(body).contains("\"error\":\"Too Many Requests\"");
    }

    @Test
    void doFilterIsolatesBucketsByRemoteAddress() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest first = new MockHttpServletRequest("POST", "/api/auth/login");
        first.setRemoteAddr("10.0.0.10");
        for (int i = 0; i < 5; i++) {
            filter.doFilter(first, new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest second = new MockHttpServletRequest("POST", "/api/auth/login");
        second.setRemoteAddr("10.0.0.11");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(second, secondResponse, chain);

        verify(chain, times(6))
                .doFilter(ArgumentMatchers.any(), ArgumentMatchers.any());
        assertThat(secondResponse.getStatus()).isEqualTo(200);
    }
}

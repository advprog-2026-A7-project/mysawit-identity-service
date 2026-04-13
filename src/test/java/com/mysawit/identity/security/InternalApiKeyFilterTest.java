package com.mysawit.identity.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InternalApiKeyFilterTest {

    private InternalApiKeyFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new InternalApiKeyFilter("test-api-key");
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotFilterNonInternalPaths() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");

        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldFilterInternalPaths() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/internal/users/1");

        assertFalse(filter.shouldNotFilter(request));
    }

    @Test
    void validKeyPassesFilter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/internal/users/1");
        request.addHeader("X-Internal-Api-Key", "test-api-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("internal-service", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    void missingKeyReturns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/internal/users/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void wrongKeyReturns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/internal/users/1");
        request.addHeader("X-Internal-Api-Key", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void emptyExpectedKeyReturns401() throws Exception {
        InternalApiKeyFilter emptyFilter = new InternalApiKeyFilter("");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/internal/users/1");
        request.addHeader("X-Internal-Api-Key", "some-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        emptyFilter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
    }

    @Test
    void nullExpectedKeyReturns401() throws Exception {
        InternalApiKeyFilter nullFilter = new InternalApiKeyFilter(null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/internal/users/1");
        request.addHeader("X-Internal-Api-Key", "some-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        nullFilter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
    }
}

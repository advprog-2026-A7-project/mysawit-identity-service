package com.mysawit.identity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.identity.config.RateLimitBucketStore;
import com.mysawit.identity.dto.ErrorResponse;
import io.github.bucket4j.Bucket;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.OffsetDateTime;

public class LoginRateLimitFilter implements Filter {

    static final String LOGIN_PATH = "/api/auth/login";
    static final String RATE_LIMIT_MESSAGE = "Too many login attempts. Please try again later.";
    private static final int SC_TOO_MANY_REQUESTS = 429;

    private final RateLimitBucketStore bucketStore;
    private final ObjectMapper objectMapper;

    public LoginRateLimitFilter(RateLimitBucketStore bucketStore, ObjectMapper objectMapper) {
        this.bucketStore = bucketStore;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String key = httpRequest.getRemoteAddr();
        Bucket bucket = bucketStore.resolveBucket(key);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }

        writeRateLimitResponse(httpRequest, httpResponse);
    }

    private void writeRateLimitResponse(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setStatus(SC_TOO_MANY_REQUESTS);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(),
                SC_TOO_MANY_REQUESTS,
                "Too Many Requests",
                RATE_LIMIT_MESSAGE,
                request.getRequestURI()
        );

        objectMapper.writeValue(response.getWriter(), body);
    }
}

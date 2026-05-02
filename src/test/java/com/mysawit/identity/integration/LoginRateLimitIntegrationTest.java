package com.mysawit.identity.integration;

import com.mysawit.identity.config.RateLimitBucketStore;
import com.mysawit.identity.fixtures.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginRateLimitIntegrationTest extends BaseIntegrationTest {

    private static final String LOGIN_URL = "/api/auth/login";

    // Ambang batas yang akan dikonfigurasi saat implementasi GREEN
    private static final int RATE_LIMIT_THRESHOLD = 5;

    @Autowired
    private RateLimitBucketStore rateLimitBucketStore;

    @BeforeEach
    void resetRateLimitState() {
        rateLimitBucketStore.clear();
    }

    @AfterEach
    void cleanUpRateLimitState() {
        rateLimitBucketStore.clear();
    }

    @Test
    void loginShouldReturn429AfterExceedingRateLimit() throws Exception {
        // Arrange: register user agar endpoint memiliki target yang valid
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestData.validRegisterRequest())))
                .andExpect(status().isCreated());

        // Payload brute-force: email valid, password salah — simulasi serangan
        String bruteForcePayload = objectMapper.writeValueAsString(
                TestData.loginRequest("test@mail.com", "WRONG_PASSWORD_BRUTE_FORCE"));

        // Act: habiskan kuota yang diizinkan (request ke-1 s.d. ke-THRESHOLD)
        for (int attempt = 1; attempt <= RATE_LIMIT_THRESHOLD; attempt++) {
            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(bruteForcePayload));
        }

        // Assert: request ke-(THRESHOLD+1) HARUS ditolak dengan HTTP 429 Too Many Requests
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bruteForcePayload))
                .andExpect(status().is(429))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").exists());
    }
}

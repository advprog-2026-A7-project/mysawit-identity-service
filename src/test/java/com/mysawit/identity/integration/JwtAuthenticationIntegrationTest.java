package com.mysawit.identity.integration;

import com.mysawit.identity.enums.Role;
import com.mysawit.identity.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JwtAuthenticationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void shouldAllowAccessWithValidJwtTokenForMandor() throws Exception {
        // Generate a real JWT token for a Mandor role
        String token = jwtTokenProvider.generateToken("123", Role.MANDOR);

        mockMvc.perform(get("/api/harvest")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource").value("harvest"))
                .andExpect(jsonPath("$.access").value("harvest-access-granted"));
    }

    @Test
    void shouldDenyAccessWithValidJwtTokenButInsufficientRole() throws Exception {
        // Generate a real JWT token for a Buruh role, but access an endpoint requiring Mandor
        String token = jwtTokenProvider.generateToken("124", Role.BURUH);

        mockMvc.perform(get("/api/harvest")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDenyAccessWithoutJwtToken() throws Exception {
        mockMvc.perform(get("/api/harvest"))
                .andExpect(status().isForbidden());
    }
}

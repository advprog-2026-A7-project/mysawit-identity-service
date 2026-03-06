package com.mysawit.identity.integration;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DomainControllerSecurityIntegrationTest extends BaseIntegrationTest {

    @Test
    void harvestRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/harvest"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANDOR")
    void harvestAllowedForMandor() throws Exception {
        mockMvc.perform(get("/api/harvest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource").value("harvest"))
                .andExpect(jsonPath("$.access").value("harvest-access-granted"));
    }

    @Test
    @WithMockUser(roles = "BURUH")
    void harvestDeniedForBuruh() throws Exception {
        mockMvc.perform(get("/api/harvest"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPIR")
    void deliveryAllowedForSupir() throws Exception {
        mockMvc.perform(get("/api/delivery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource").value("delivery"))
                .andExpect(jsonPath("$.access").value("delivery-access-granted"));
    }

    @Test
    @WithMockUser(roles = "MANDOR")
    void deliveryDeniedForMandor() throws Exception {
        mockMvc.perform(get("/api/delivery"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void payrollAllowedForAdmin() throws Exception {
        mockMvc.perform(get("/api/payroll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource").value("payroll"))
                .andExpect(jsonPath("$.access").value("payroll-access-granted"));
    }

    @Test
    @WithMockUser(roles = "MANDOR")
    void payrollDeniedForMandor() throws Exception {
        mockMvc.perform(get("/api/payroll"))
                .andExpect(status().isForbidden());
    }
}

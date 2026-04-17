package com.mysawit.identity.integration;

import com.mysawit.identity.fixtures.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void registerSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestData.validRegisterRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@mail.com"))
                .andExpect(jsonPath("$.role").value("BURUH"))
                .andExpect(jsonPath("$.googleLinked").value(false))
                .andExpect(jsonPath("$.hasPassword").value(true));
    }

    @Test
    void registerDuplicateEmailReturnsConflict() throws Exception {
        String json = objectMapper.writeValueAsString(TestData.validRegisterRequest());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void registerInvalidPayload() throws Exception {
        String json = objectMapper.writeValueAsString(TestData.registerRequest("ab", "invalid", "123"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void registerAdminRoleForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestData.adminRegisterRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot self-register as ADMIN"));
    }

    @Test
    void registerMandorSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestData.mandorRegisterRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MANDOR"));
    }

    @Test
    void registerMandorFailsWhenCertificationMissing() throws Exception {
        var request = TestData.mandorRegisterRequest();
        request.setCertificationNumber(" ");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Certification number is required for MANDOR"));
    }

    @Test
    void registerMandorDuplicateCertificationReturnsConflict() throws Exception {
        var request = TestData.mandorRegisterRequest();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        var duplicate = TestData.mandorRegisterRequest();
        duplicate.setUsername("mandor2");
        duplicate.setEmail("mandor2@mail.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Certification number already exists"));
    }

    @Test
    void registerSupirSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestData.supirRegisterRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("SUPIR"));
    }

    @Test
    void registerBuruhWithMandorSuccess() throws Exception {
        MvcResult mandorResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestData.mandorRegisterRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MANDOR"))
                .andReturn();

        String mandorId = objectMapper.readTree(mandorResult.getResponse().getContentAsString())
                .get("id").asText();

        var buruhRequest = TestData.buruhWithMandorRequest(mandorId);
        buruhRequest.setEmail("buruh@mail.com");
        buruhRequest.setUsername("buruh");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buruhRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("BURUH"));
    }

    @Test
    void registerBuruhWithInvalidMandorFails() throws Exception {
        var request = TestData.buruhWithMandorRequest("missing-id");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid mandorId"));
    }

    @Test
    void loginSuccessWithEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestData.validRegisterRequest())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestData.validLoginRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@mail.com"))
                .andExpect(jsonPath("$.role").value("BURUH"))
                .andExpect(jsonPath("$.googleLinked").value(false))
                .andExpect(jsonPath("$.hasPassword").value(true));
    }

    @Test
    void loginSuccessWithUsernameAlias() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestData.validRegisterRequest())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "test@mail.com",
                                "password", "secret123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@mail.com"));
    }

    @Test
    void loginWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestData.validRegisterRequest())))
                .andExpect(status().isCreated());

        String loginJson = objectMapper.writeValueAsString(
                TestData.loginRequest("test@mail.com", "wrongpassword"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginUserNotFound() throws Exception {
        String loginJson = objectMapper.writeValueAsString(
                TestData.loginRequest("nonexistent@mail.com", "secret123"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginAdminSeededBySqlSuccess() throws Exception {
        String adminId = "admin-0001";
        String password = "adminSecret123";
        String encodedPassword = new BCryptPasswordEncoder().encode(password);

        jdbcTemplate.update("""
                INSERT INTO users
                (id, username, email, name, password, role, account_non_expired, account_non_locked, credentials_non_expired, enabled, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, adminId, "admin", "admin@mail.com", "admin", encodedPassword, "ADMIN", true, true, true, true);

        jdbcTemplate.update("INSERT INTO admins (id) VALUES (?)", adminId);

        String loginJson = objectMapper.writeValueAsString(
                TestData.loginRequest("admin@mail.com", password));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void validateTokenSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestData.validRegisterRequest())))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestData.validLoginRequest())))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();
        String userId = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(post("/api/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.username").value(userId));
    }

    @Test
    void validateTokenInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", "invalid-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }

    @Test
    void validateTokenMissingToken() throws Exception {
        mockMvc.perform(post("/api/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void healthCheck() throws Exception {
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("mysawit-identity-service"));
    }
}

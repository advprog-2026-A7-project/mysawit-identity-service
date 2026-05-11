package com.mysawit.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.identity.dto.AuthResponse;
import com.mysawit.identity.dto.LoginRequest;
import com.mysawit.identity.dto.RegisterRequest;
import com.mysawit.identity.dto.ValidateTokenResponse;
import com.mysawit.identity.enums.Role;
import com.mysawit.identity.exception.DuplicateEmailException;
import com.mysawit.identity.exception.InvalidCredentialsException;
import com.mysawit.identity.exception.InvalidTokenException;
import com.mysawit.identity.exception.MissingMandorCertificationException;
import com.mysawit.identity.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerReturns201WithUserResponseShape() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("user");
        request.setEmail("user@mail.com");
        request.setPassword("secret123");
        request.setRole(Role.BURUH);

        AuthResponse response = new AuthResponse("jwt-token", "refresh-token", "1", "user", "user@mail.com", "BURUH");
        response.setGoogleLinked(false);
        response.setHasPassword(true);
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.email").value("user@mail.com"))
                .andExpect(jsonPath("$.role").value("BURUH"))
                .andExpect(jsonPath("$.googleLinked").value(false))
                .andExpect(jsonPath("$.hasPassword").value(true));
    }

    @Test
    void registerReturns400ForInvalidPayload() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("ab");
        request.setEmail("invalid-email");
        request.setPassword("123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/auth/register"));
    }

    @Test
    void registerReturns409ForDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing-user");
        request.setEmail("existing@mail.com");
        request.setPassword("secret123");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateEmailException("Email already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void registerReturns400ForMissingMandorCertification() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("mandor");
        request.setEmail("mandor@mail.com");
        request.setPassword("secret123");
        request.setRole(Role.MANDOR);

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new MissingMandorCertificationException("Certification number is required for MANDOR"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Certification number is required for MANDOR"));
    }

    @Test
    void loginReturns200WithLoginResponse() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@mail.com");
        request.setPassword("secret123");

        AuthResponse response = new AuthResponse("jwt-token", "refresh-token", "1", "user", "user@mail.com", "BURUH");
        response.setGoogleLinked(false);
        response.setHasPassword(true);
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.email").value("user@mail.com"))
                .andExpect(jsonPath("$.role").value("BURUH"))
                .andExpect(jsonPath("$.googleLinked").value(false))
                .andExpect(jsonPath("$.hasPassword").value(true));
    }

    @Test
    void loginAcceptsUsernameAliasForBackwardCompatibility() throws Exception {
        AuthResponse response = new AuthResponse("jwt-token", "refresh-token", "1", "user", "user@mail.com", "BURUH");
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "user@mail.com",
                                "password", "secret123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@mail.com"));
    }

    @Test
    void loginReturns401ForInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@mail.com");
        request.setPassword("wrong-password");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void validateReturns200ForValidToken() throws Exception {
        when(authService.validateToken("valid-jwt"))
                .thenReturn(new ValidateTokenResponse(true, "user"));

        mockMvc.perform(post("/api/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", "valid-jwt"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.username").value("user"));
    }

    @Test
    void validateReturns401ForInvalidToken() throws Exception {
        when(authService.validateToken("invalid-jwt"))
                .thenThrow(new InvalidTokenException("Invalid or expired token"));

        mockMvc.perform(post("/api/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", "invalid-jwt"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }

    @Test
    void validateReturns400WhenTokenMissingOrBlank() throws Exception {
        mockMvc.perform(post("/api/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void healthReturns200WithServiceStatus() throws Exception {
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("mysawit-identity-service"));
    }
}

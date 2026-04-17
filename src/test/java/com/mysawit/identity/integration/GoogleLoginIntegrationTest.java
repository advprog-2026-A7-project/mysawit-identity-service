package com.mysawit.identity.integration;

import com.mysawit.identity.dto.GoogleLoginRequest;
import com.mysawit.identity.dto.GoogleUserInfo;
import com.mysawit.identity.enums.Role;
import com.mysawit.identity.model.Buruh;
import com.mysawit.identity.repository.UserRepository;
import com.mysawit.identity.service.GoogleTokenVerifierService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import com.mysawit.identity.security.JwtTokenProvider;

import java.time.LocalDateTime;

class GoogleLoginIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private GoogleTokenVerifierService googleTokenVerifierService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void googleLogin_returnsToken_whenNewUser() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("valid-token");
        request.setRole(Role.BURUH);
        request.setUsername("Google User");

        GoogleUserInfo mockUserInfo = GoogleUserInfo.builder()
                .googleSub("google-123")
                .email("test@google.com")
                .name("Google User")
                .build();

        when(googleTokenVerifierService.verifyToken("valid-token")).thenReturn(mockUserInfo);

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("test@google.com"))
                .andExpect(jsonPath("$.role").value("BURUH"))
                .andExpect(jsonPath("$.username").value("Google User"))
                .andExpect(jsonPath("$.googleLinked").value(true))
                .andExpect(jsonPath("$.hasPassword").value(false));
    }

    @Test
    void googleLogin_returnsToken_whenExistingGoogleSub() throws Exception {
        Buruh user = new Buruh();
        user.setEmail("test2@google.com");
        user.setName("Existing User");
        user.setUsername("Existing User");
        user.setPassword("secret");
        user.setRole(Role.BURUH);
        user.setGoogleSub("google-456");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("valid-token-2");

        GoogleUserInfo mockUserInfo = GoogleUserInfo.builder()
                .googleSub("google-456")
                .email("test2@google.com")
                .name("New Name")
                .build();

        when(googleTokenVerifierService.verifyToken("valid-token-2")).thenReturn(mockUserInfo);

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value("test2@google.com"))
                .andExpect(jsonPath("$.role").value("BURUH"))
                .andExpect(jsonPath("$.username").value("Existing User"))
                .andExpect(jsonPath("$.googleLinked").value(true))
                .andExpect(jsonPath("$.hasPassword").value(true));
    }

    @Test
    void googleLogin_returns400_whenNewUserMissingRole() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("valid-token-norole");
        request.setUsername("Some User");

        GoogleUserInfo mockUserInfo = GoogleUserInfo.builder()
                .googleSub("google-norole")
                .email("norole@google.com")
                .name("Some User")
                .build();

        when(googleTokenVerifierService.verifyToken("valid-token-norole")).thenReturn(mockUserInfo);

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void googleLogin_returns400_whenNewUserMissingUsername() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("valid-token-nousername");
        request.setRole(Role.BURUH);

        GoogleUserInfo mockUserInfo = GoogleUserInfo.builder()
                .googleSub("google-nousername")
                .email("nousername@google.com")
                .name("Some User")
                .build();

        when(googleTokenVerifierService.verifyToken("valid-token-nousername")).thenReturn(mockUserInfo);

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void googleLogin_returns401_whenInvalidToken() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("invalid-token");

        when(googleTokenVerifierService.verifyToken("invalid-token"))
                .thenThrow(new com.mysawit.identity.exception.InvalidTokenException("Invalid Google token"));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void googleLogin_returnsValidJwtWithExpectedClaims_whenNewUser() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("valid-token-3");
        request.setRole(Role.BURUH);
        request.setUsername("Google User 3");

        GoogleUserInfo mockUserInfo = GoogleUserInfo.builder()
                .googleSub("google-789")
                .email("test3@google.com")
                .name("Google User 3")
                .build();

        when(googleTokenVerifierService.verifyToken("valid-token-3")).thenReturn(mockUserInfo);

        MvcResult result = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Extract token and id from response
        String responseContent = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseContent).get("token").asText();
        String returnedUserId = objectMapper.readTree(responseContent).get("id").asText();
        String returnedRole = objectMapper.readTree(responseContent).get("role").asText();

        // Parse the JWT using JwtTokenProvider's signing key
        javax.crypto.SecretKey secretKey = (javax.crypto.SecretKey) ReflectionTestUtils.invokeMethod(jwtTokenProvider, "getSigningKey");
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Assert claims
        assertNotNull(claims);
        assertEquals(returnedUserId, claims.getSubject());
        assertEquals(returnedUserId, claims.get("userId"));
        assertEquals(returnedRole, claims.get("role"));
    }
}

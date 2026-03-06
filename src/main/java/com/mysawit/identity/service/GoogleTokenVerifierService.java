package com.mysawit.identity.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.mysawit.identity.dto.GoogleUserInfo;
import com.mysawit.identity.exception.InvalidTokenException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;

@Slf4j
@Service
public class GoogleTokenVerifierService {

    @Value("${google.client-id:}")
    private String clientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(clientId)) {
            log.warn("Google Client ID is not configured (google.client-id is missing or empty)");
        }
        
        verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public GoogleUserInfo verifyToken(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                
                String googleSub = payload.getSubject();
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                
                return GoogleUserInfo.builder()
                        .googleSub(googleSub)
                        .email(email)
                        .name(name)
                        .build();
            } else {
                throw new InvalidTokenException("Invalid Google ID token");
            }
        } catch (Exception e) {
            log.error("Google token verification failed", e);
            throw new InvalidTokenException("Invalid Google ID token");
        }
    }
}

package com.mysawit.identity.dto;

import jakarta.validation.constraints.NotBlank;

public class ValidateTokenRequest {

    @NotBlank(message = "Token is required")
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

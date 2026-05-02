package com.mysawit.identity.dto;

import com.mysawit.identity.enums.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    @NotBlank(message = "ID token is required")
    private String idToken;

    private String username;
    private Role role;
    private String certificationNumber;
}

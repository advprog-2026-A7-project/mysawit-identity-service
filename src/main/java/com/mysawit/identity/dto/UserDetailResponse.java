package com.mysawit.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailResponse {
    private String id;
    private String username;
    private String email;
    private String name;
    private String role;
    private LocalDateTime createdAt;
    private String mandorId;
    private String certificationNumber;
    private String kebunId;
}

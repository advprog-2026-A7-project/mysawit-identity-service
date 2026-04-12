package com.mysawit.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalUserDetailResponse {
    private String id;
    private String name;
    private String email;
    private String role;
    private String mandorId;
    private String mandorName;
    private String certificationNumber;
    private String kebunId;
}

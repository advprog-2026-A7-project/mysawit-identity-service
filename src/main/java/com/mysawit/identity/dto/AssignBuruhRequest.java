package com.mysawit.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignBuruhRequest {

    @NotBlank(message = "Mandor ID is required")
    private String mandorId;
}

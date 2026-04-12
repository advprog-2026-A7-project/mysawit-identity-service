package com.mysawit.identity.controller;

import com.mysawit.identity.dto.InternalUserDetailResponse;
import com.mysawit.identity.service.InternalUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/users")
public class InternalUserController {

    private final InternalUserService internalUserService;

    public InternalUserController(InternalUserService internalUserService) {
        this.internalUserService = internalUserService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<InternalUserDetailResponse> getUserById(@PathVariable String userId) {
        InternalUserDetailResponse response = internalUserService.getUserById(userId);
        return ResponseEntity.ok(response);
    }
}

package com.mysawit.identity.fixtures;

import com.mysawit.identity.dto.LoginRequest;
import com.mysawit.identity.dto.RegisterRequest;
import com.mysawit.identity.enums.Role;

public final class TestData {

    private TestData() {
    }

    public static RegisterRequest validRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@mail.com");
        request.setPassword("secret123");
        return request;
    }

    public static RegisterRequest registerRequest(String username, String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    public static RegisterRequest adminRegisterRequest() {
        RegisterRequest request = validRegisterRequest();
        request.setRole(Role.ADMIN);
        return request;
    }

    public static LoginRequest validLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("test@mail.com");
        request.setPassword("secret123");
        return request;
    }

    public static LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(email);
        request.setPassword(password);
        return request;
    }
}

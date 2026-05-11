package com.mysawit.identity.service.registration;

import com.mysawit.identity.enums.Role;
import com.mysawit.identity.exception.InvalidRoleRegistrationException;
import com.mysawit.identity.model.User;
import org.springframework.stereotype.Component;

@Component
public class AdminRegistrationStrategy implements UserRegistrationStrategy {

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    @Override
    public User createUser(UserCreationContext context) {
        throw new InvalidRoleRegistrationException("Cannot self-register as ADMIN");
    }
}

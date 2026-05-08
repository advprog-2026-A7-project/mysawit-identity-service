package com.mysawit.identity.service.registration;

import com.mysawit.identity.enums.Role;
import com.mysawit.identity.exception.InvalidRoleRegistrationException;
import com.mysawit.identity.model.User;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class UserRegistrationFactory {

    private final Map<Role, UserRegistrationStrategy> strategies;

    public UserRegistrationFactory(List<UserRegistrationStrategy> strategyBeans) {
        this.strategies = new EnumMap<>(Role.class);
        for (UserRegistrationStrategy strategy : strategyBeans) {
            this.strategies.put(strategy.getRole(), strategy);
        }
    }

    public User create(Role role, UserCreationContext context) {
        UserRegistrationStrategy strategy = strategies.get(role);
        if (strategy == null) {
            throw new InvalidRoleRegistrationException("Unsupported role for registration: " + role);
        }
        return strategy.createUser(context);
    }
}

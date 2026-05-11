package com.mysawit.identity.service.registration;

import com.mysawit.identity.enums.Role;
import com.mysawit.identity.model.Supir;
import com.mysawit.identity.model.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SupirRegistrationStrategy implements UserRegistrationStrategy {

    @Override
    public Role getRole() {
        return Role.SUPIR;
    }

    @Override
    public User createUser(UserCreationContext context) {
        Supir supir = new Supir();
        supir.setKebunId(trimToNull(context.getKebunId()));
        return supir;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}

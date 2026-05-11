package com.mysawit.identity.service.registration;

import com.mysawit.identity.enums.Role;
import com.mysawit.identity.model.User;

public interface UserRegistrationStrategy {

    Role getRole();

    User createUser(UserCreationContext context);
}

package com.mysawit.identity.service.registration;

import com.mysawit.identity.enums.Role;
import com.mysawit.identity.exception.InvalidRoleRegistrationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminRegistrationStrategyTest {

    private final AdminRegistrationStrategy strategy = new AdminRegistrationStrategy();

    @Test
    void getRoleReturnsAdmin() {
        assertEquals(Role.ADMIN, strategy.getRole());
    }

    @Test
    void createUserAlwaysThrows() {
        UserCreationContext context = UserCreationContext.builder().build();

        InvalidRoleRegistrationException exception = assertThrows(
                InvalidRoleRegistrationException.class,
                () -> strategy.createUser(context)
        );
        assertEquals("Cannot self-register as ADMIN", exception.getMessage());
    }
}

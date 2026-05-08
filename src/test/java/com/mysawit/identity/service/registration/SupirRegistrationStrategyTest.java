package com.mysawit.identity.service.registration;

import com.mysawit.identity.enums.Role;
import com.mysawit.identity.model.Supir;
import com.mysawit.identity.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SupirRegistrationStrategyTest {

    private SupirRegistrationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new SupirRegistrationStrategy();
    }

    @Test
    void getRoleReturnsSupir() {
        assertEquals(Role.SUPIR, strategy.getRole());
    }

    @Test
    void createUserSetsKebunIdWhenProvided() {
        UserCreationContext context = UserCreationContext.builder().kebunId(" kebun-1 ").build();

        User user = strategy.createUser(context);

        assertInstanceOf(Supir.class, user);
        assertEquals("kebun-1", ((Supir) user).getKebunId());
    }

    @Test
    void createUserHasNullKebunIdWhenMissing() {
        UserCreationContext context = UserCreationContext.builder().build();

        User user = strategy.createUser(context);

        assertInstanceOf(Supir.class, user);
        assertNull(((Supir) user).getKebunId());
    }

    @Test
    void createUserHasNullKebunIdWhenBlank() {
        UserCreationContext context = UserCreationContext.builder().kebunId("   ").build();

        User user = strategy.createUser(context);

        assertInstanceOf(Supir.class, user);
        assertNull(((Supir) user).getKebunId());
    }
}

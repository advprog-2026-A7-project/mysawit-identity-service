package com.mysawit.identity.service.registration;

import com.mysawit.identity.enums.Role;
import com.mysawit.identity.exception.InvalidRoleRegistrationException;
import com.mysawit.identity.model.Buruh;
import com.mysawit.identity.model.User;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserRegistrationFactoryTest {

    @Test
    void createDelegatesToMatchingStrategy() {
        Buruh expected = new Buruh();
        UserRegistrationStrategy buruhStrategy = mock(UserRegistrationStrategy.class);
        when(buruhStrategy.getRole()).thenReturn(Role.BURUH);
        UserCreationContext context = UserCreationContext.builder().build();
        when(buruhStrategy.createUser(context)).thenReturn(expected);

        UserRegistrationFactory factory = new UserRegistrationFactory(List.of(buruhStrategy));

        User result = factory.create(Role.BURUH, context);

        assertSame(expected, result);
        verify(buruhStrategy).createUser(context);
    }

    @Test
    void createThrowsWhenStrategyMissing() {
        UserRegistrationFactory factory = new UserRegistrationFactory(Collections.emptyList());

        InvalidRoleRegistrationException exception = assertThrows(
                InvalidRoleRegistrationException.class,
                () -> factory.create(Role.BURUH, UserCreationContext.builder().build())
        );
        assertTrue(exception.getMessage().contains("Unsupported role"));
    }

    @Test
    void laterStrategyOverridesEarlierForSameRole() {
        UserRegistrationStrategy first = mock(UserRegistrationStrategy.class);
        when(first.getRole()).thenReturn(Role.SUPIR);
        UserRegistrationStrategy second = mock(UserRegistrationStrategy.class);
        when(second.getRole()).thenReturn(Role.SUPIR);

        UserRegistrationFactory factory = new UserRegistrationFactory(List.of(first, second));
        UserCreationContext context = UserCreationContext.builder().build();
        factory.create(Role.SUPIR, context);

        verify(second).createUser(context);
        verify(first, never()).createUser(any());
    }
}

package com.mysawit.identity.service.registration;

import com.mysawit.identity.enums.Role;
import com.mysawit.identity.exception.InvalidMandorException;
import com.mysawit.identity.model.Buruh;
import com.mysawit.identity.model.Mandor;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.MandorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BuruhRegistrationStrategyTest {

    private MandorRepository mandorRepository;
    private BuruhRegistrationStrategy strategy;

    @BeforeEach
    void setUp() {
        mandorRepository = mock(MandorRepository.class);
        strategy = new BuruhRegistrationStrategy(mandorRepository);
    }

    @Test
    void getRoleReturnsBuruh() {
        assertEquals(Role.BURUH, strategy.getRole());
    }

    @Test
    void createUserWithoutMandorIdProducesPlainBuruh() {
        UserCreationContext context = UserCreationContext.builder().build();

        User user = strategy.createUser(context);

        assertInstanceOf(Buruh.class, user);
        assertNull(((Buruh) user).getMandor());
        verifyNoInteractions(mandorRepository);
    }

    @Test
    void createUserWithBlankMandorIdProducesPlainBuruh() {
        UserCreationContext context = UserCreationContext.builder().mandorId("   ").build();

        User user = strategy.createUser(context);

        assertInstanceOf(Buruh.class, user);
        assertNull(((Buruh) user).getMandor());
        verifyNoInteractions(mandorRepository);
    }

    @Test
    void createUserWithValidMandorIdAttachesMandor() {
        Mandor mandor = new Mandor();
        mandor.setId("mandor-1");
        when(mandorRepository.findById("mandor-1")).thenReturn(Optional.of(mandor));

        UserCreationContext context = UserCreationContext.builder().mandorId(" mandor-1 ").build();

        User user = strategy.createUser(context);

        assertInstanceOf(Buruh.class, user);
        assertSame(mandor, ((Buruh) user).getMandor());
    }

    @Test
    void createUserThrowsWhenMandorIdInvalid() {
        when(mandorRepository.findById("missing")).thenReturn(Optional.empty());

        UserCreationContext context = UserCreationContext.builder().mandorId("missing").build();

        InvalidMandorException exception = assertThrows(
                InvalidMandorException.class,
                () -> strategy.createUser(context)
        );
        assertEquals("Invalid mandorId", exception.getMessage());
    }
}

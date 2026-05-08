package com.mysawit.identity.service.registration;

import com.mysawit.identity.enums.Role;
import com.mysawit.identity.exception.DuplicateCertificationNumberException;
import com.mysawit.identity.exception.MissingMandorCertificationException;
import com.mysawit.identity.model.Mandor;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.MandorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MandorRegistrationStrategyTest {

    private MandorRepository mandorRepository;
    private MandorRegistrationStrategy strategy;

    @BeforeEach
    void setUp() {
        mandorRepository = mock(MandorRepository.class);
        strategy = new MandorRegistrationStrategy(mandorRepository);
    }

    @Test
    void getRoleReturnsMandor() {
        assertEquals(Role.MANDOR, strategy.getRole());
    }

    @Test
    void createUserThrowsWhenCertificationMissing() {
        UserCreationContext context = UserCreationContext.builder().build();

        MissingMandorCertificationException exception = assertThrows(
                MissingMandorCertificationException.class,
                () -> strategy.createUser(context)
        );
        assertEquals("Certification number is required for MANDOR", exception.getMessage());
        verifyNoInteractions(mandorRepository);
    }

    @Test
    void createUserThrowsWhenCertificationBlank() {
        UserCreationContext context = UserCreationContext.builder().certificationNumber("   ").build();

        assertThrows(MissingMandorCertificationException.class, () -> strategy.createUser(context));
        verifyNoInteractions(mandorRepository);
    }

    @Test
    void createUserThrowsWhenCertificationDuplicate() {
        when(mandorRepository.existsByCertificationNumber("CERT-1")).thenReturn(true);
        UserCreationContext context = UserCreationContext.builder().certificationNumber("CERT-1").build();

        DuplicateCertificationNumberException exception = assertThrows(
                DuplicateCertificationNumberException.class,
                () -> strategy.createUser(context)
        );
        assertEquals("Certification number already exists", exception.getMessage());
    }

    @Test
    void createUserReturnsMandorWhenValid() {
        when(mandorRepository.existsByCertificationNumber("CERT-1")).thenReturn(false);
        UserCreationContext context = UserCreationContext.builder().certificationNumber("  CERT-1  ").build();

        User user = strategy.createUser(context);

        assertInstanceOf(Mandor.class, user);
        assertEquals("CERT-1", ((Mandor) user).getCertificationNumber());
    }
}

package com.mysawit.identity.service.registration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserCreationContextTest {

    @Test
    void builderPopulatesAllFields() {
        UserCreationContext context = UserCreationContext.builder()
                .certificationNumber("CERT-1")
                .mandorId("mandor-1")
                .kebunId("kebun-1")
                .build();

        assertEquals("CERT-1", context.getCertificationNumber());
        assertEquals("mandor-1", context.getMandorId());
        assertEquals("kebun-1", context.getKebunId());
    }

    @Test
    void builderProducesNullsForUnsetFields() {
        UserCreationContext context = UserCreationContext.builder().build();

        assertNull(context.getCertificationNumber());
        assertNull(context.getMandorId());
        assertNull(context.getKebunId());
    }

    @Test
    void builderReturnsSameInstanceForChaining() {
        UserCreationContext.Builder builder = UserCreationContext.builder();
        assertSame(builder, builder.certificationNumber("X"));
        assertSame(builder, builder.mandorId("Y"));
        assertSame(builder, builder.kebunId("Z"));
    }
}

package com.mysawit.identity.service;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DomainOperationsServiceMethodSecurityTest {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private DomainAuthorizationService domainAuthorizationService;

    @Autowired
    private DomainOperationsService domainOperationsService;

    @Test
    void protectedMethodsArePublicAndBeanIsProxied() throws Exception {
        assertTrue(AopUtils.isAopProxy(domainAuthorizationService));

        Method harvest = DomainAuthorizationService.class.getMethod("authorizeHarvestAccess");
        Method delivery = DomainAuthorizationService.class.getMethod("authorizeDeliveryAccess");
        Method payroll = DomainAuthorizationService.class.getMethod("authorizePayrollAccess");

        assertTrue(Modifier.isPublic(harvest.getModifiers()));
        assertTrue(Modifier.isPublic(delivery.getModifiers()));
        assertTrue(Modifier.isPublic(payroll.getModifiers()));
    }

    @Test
    @WithMockUser(roles = "MANDOR")
    void harvestAllowedForMandorViaBeanProxy() {
        assertEquals("harvest-access-granted", domainOperationsService.harvest());
    }

    @Test
    @WithMockUser(roles = "BURUH")
    void harvestDeniedForBuruhViaBeanProxy() {
        assertThrows(AccessDeniedException.class, () -> domainOperationsService.harvest());
    }

    @Test
    @WithMockUser(roles = "SUPIR")
    void deliveryAllowedForSupirViaBeanProxy() {
        assertEquals("delivery-access-granted", domainOperationsService.delivery());
    }

    @Test
    @WithMockUser(roles = "MANDOR")
    void payrollDeniedForMandorViaBeanProxy() {
        assertThrows(AccessDeniedException.class, () -> domainOperationsService.payroll());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void payrollAllowedForAdminViaBeanProxy() {
        assertEquals("payroll-access-granted", domainOperationsService.payroll());
    }
}

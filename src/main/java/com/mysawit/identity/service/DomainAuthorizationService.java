package com.mysawit.identity.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class DomainAuthorizationService {

    @PreAuthorize("hasAnyRole('MANDOR','ADMIN')")
    public String authorizeHarvestAccess() {
        return "harvest-access-granted";
    }

    @PreAuthorize("hasAnyRole('SUPIR','ADMIN')")
    public String authorizeDeliveryAccess() {
        return "delivery-access-granted";
    }

    @PreAuthorize("hasRole('ADMIN')")
    public String authorizePayrollAccess() {
        return "payroll-access-granted";
    }
}

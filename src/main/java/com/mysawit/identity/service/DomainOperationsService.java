package com.mysawit.identity.service;

import org.springframework.stereotype.Service;

@Service
public class DomainOperationsService {

    private final DomainAuthorizationService domainAuthorizationService;

    public DomainOperationsService(DomainAuthorizationService domainAuthorizationService) {
        this.domainAuthorizationService = domainAuthorizationService;
    }

    public String harvest() {
        return domainAuthorizationService.authorizeHarvestAccess();
    }

    public String delivery() {
        return domainAuthorizationService.authorizeDeliveryAccess();
    }

    public String payroll() {
        return domainAuthorizationService.authorizePayrollAccess();
    }
}

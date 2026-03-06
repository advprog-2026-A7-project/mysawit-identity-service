package com.mysawit.identity.controller;

import com.mysawit.identity.service.DomainOperationsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DomainController {

    private static final String RESOURCE_KEY = "resource";
    private static final String ACCESS_KEY = "access";

    private final DomainOperationsService domainOperationsService;

    public DomainController(DomainOperationsService domainOperationsService) {
        this.domainOperationsService = domainOperationsService;
    }

    @GetMapping("/harvest")
    public ResponseEntity<Map<String, String>> harvest() {
        return ResponseEntity.ok(Map.of(
                RESOURCE_KEY, "harvest",
                ACCESS_KEY, domainOperationsService.harvest()
        ));
    }

    @GetMapping("/delivery")
    public ResponseEntity<Map<String, String>> delivery() {
        return ResponseEntity.ok(Map.of(
                RESOURCE_KEY, "delivery",
                ACCESS_KEY, domainOperationsService.delivery()
        ));
    }

    @GetMapping("/payroll")
    public ResponseEntity<Map<String, String>> payroll() {
        return ResponseEntity.ok(Map.of(
                RESOURCE_KEY, "payroll",
                ACCESS_KEY, domainOperationsService.payroll()
        ));
    }
}

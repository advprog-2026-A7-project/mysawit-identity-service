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

    private final DomainOperationsService domainOperationsService;

    public DomainController(DomainOperationsService domainOperationsService) {
        this.domainOperationsService = domainOperationsService;
    }

    @GetMapping("/harvest")
    public ResponseEntity<Map<String, String>> harvest() {
        return ResponseEntity.ok(Map.of(
                "resource", "harvest",
                "access", domainOperationsService.harvest()
        ));
    }

    @GetMapping("/delivery")
    public ResponseEntity<Map<String, String>> delivery() {
        return ResponseEntity.ok(Map.of(
                "resource", "delivery",
                "access", domainOperationsService.delivery()
        ));
    }

    @GetMapping("/payroll")
    public ResponseEntity<Map<String, String>> payroll() {
        return ResponseEntity.ok(Map.of(
                "resource", "payroll",
                "access", domainOperationsService.payroll()
        ));
    }
}

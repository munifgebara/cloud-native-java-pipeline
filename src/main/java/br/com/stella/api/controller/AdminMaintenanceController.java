package br.com.stella.api.controller;

import br.com.stella.api.dto.InactiveRecordsPurgeResultDTO;
import br.com.stella.api.service.InactiveRecordsPurgeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrative operations for maintaining Stella's operational data.
 */
@RestController
@RequestMapping("/api/v0/admin/maintenance")
public class AdminMaintenanceController {

    private final InactiveRecordsPurgeService service;

    public AdminMaintenanceController(InactiveRecordsPurgeService service) {
        this.service = service;
    }

    @DeleteMapping("/inactive-records")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<InactiveRecordsPurgeResultDTO> purgeInactiveRecords() {
        return ResponseEntity.ok(service.purge());
    }
}

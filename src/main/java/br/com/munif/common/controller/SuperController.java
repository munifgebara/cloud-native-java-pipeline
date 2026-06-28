package br.com.munif.common.controller;

import br.com.munif.common.service.CrudOperations;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

/**
 * Base implementation of the standard REST CRUD endpoints.
 *
 * <p>Concrete controllers define only their resource path and specific endpoints.
 * Persistence access remains in the service, where owner isolation is enforced by
 * the custom repository.</p>
 *
 * @param <SUMMARY> summary DTO used in listings
 * @param <RESPONSE> full resource DTO
 * @param <CREATE> creation DTO
 * @param <UPDATE> update DTO
 * @param <REVISION> revision DTO
 */
public abstract class SuperController<SUMMARY, RESPONSE, CREATE, UPDATE, REVISION> {

    private final CrudOperations<SUMMARY, RESPONSE, CREATE, UPDATE, REVISION> service;

    protected SuperController(CrudOperations<SUMMARY, RESPONSE, CREATE, UPDATE, REVISION> service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RESPONSE> create(@RequestBody @Valid CREATE dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RESPONSE> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findResponseById(id));
    }

    @GetMapping
    public ResponseEntity<List<SUMMARY>> list() {
        return ResponseEntity.ok(service.listSummary());
    }

    @PutMapping("/{id}")
    public ResponseEntity<RESPONSE> update(@PathVariable UUID id, @RequestBody @Valid UPDATE dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteLogically(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    public ResponseEntity<List<SUMMARY>> findAllIncludingInactive() {
        return ResponseEntity.ok(service.listSummaryIncludingInactive());
    }

    @GetMapping("/{id}/revisions")
    public ResponseEntity<List<REVISION>> listPreviousVersions(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listRevisions(id));
    }
}

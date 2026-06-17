package br.com.stella.api.controller;

import br.com.munif.common.controller.SuperController;
import br.com.stella.api.dto.PersonCreateDTO;
import br.com.stella.api.dto.PersonResponseDTO;
import br.com.stella.api.dto.PersonRevisionDTO;
import br.com.stella.api.dto.PersonSummaryDTO;
import br.com.stella.api.dto.PersonUpdateDTO;
import br.com.stella.api.entity.Person;
import br.com.stella.api.service.PersonService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing persons (individuals or legal entities).
 *
 * <p>Exposes the {@code /api/v0/people} resource with CRUD, name search,
 * and audit revision history.</p>
 */
@RestController
@RequestMapping("/api/v0/people")
public class PersonController extends SuperController<PersonSummaryDTO, PersonResponseDTO, PersonCreateDTO, PersonUpdateDTO, Person> {

    private final PersonService service;

    /**
     * Constructs the controller injecting the person business service.
     *
     * @param service person service
     */
    public PersonController(PersonService service) {
        this.service = service;
    }

    @Override
    @PostMapping
    public ResponseEntity<PersonResponseDTO> create(@RequestBody @Valid PersonCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<PersonResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findResponseById(id));
    }

    @Override
    @GetMapping
    public ResponseEntity<List<PersonSummaryDTO>> listar() {
        return ResponseEntity.ok(service.listSummary());
    }

    /**
     * Finds active persons whose name contains the given text (partial, case-insensitive search).
     *
     * @param name substring to search in the person's name
     * @return {@code 200 OK} with the list of found persons
     */
    @GetMapping("/search")
    public ResponseEntity<List<PersonSummaryDTO>> findByName(@RequestParam String name) {
        return ResponseEntity.ok(service.findByName(name));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<PersonResponseDTO> update(@PathVariable UUID id, @RequestBody @Valid PersonUpdateDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteLogically(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/todos")
    public ResponseEntity<List<PersonSummaryDTO>> findAllIncludingInactive() {
        return ResponseEntity.ok(service.listSummaryIncludingInactive());
    }

    @Override
    @GetMapping("/{id}/revisoes")
    public ResponseEntity<List<PersonRevisionDTO>> listPreviousVersions(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listRevisions(id));
    }
}

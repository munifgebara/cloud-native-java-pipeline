package br.com.stella.api.controller;

import br.com.munif.common.controller.SuperController;
import br.com.stella.api.dto.PersonCreateDTO;
import br.com.stella.api.dto.PersonResponseDTO;
import br.com.stella.api.dto.PersonRevisionDTO;
import br.com.stella.api.dto.PersonSummaryDTO;
import br.com.stella.api.dto.PersonUpdateDTO;
import br.com.stella.api.entity.Person;
import br.com.stella.api.service.PersonService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
public class PersonController extends SuperController<PersonSummaryDTO, PersonResponseDTO, PersonCreateDTO, PersonUpdateDTO, PersonRevisionDTO> {

    private final PersonService service;

    /**
     * Constructs the controller injecting the person business service.
     *
     * @param service person service
     */
    public PersonController(PersonService service) {
        super(service);
        this.service = service;
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

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PersonResponseDTO> updatePhoto(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(service.updatePhoto(id, file));
    }

    @DeleteMapping("/{id}/photo")
    public ResponseEntity<PersonResponseDTO> removePhoto(@PathVariable UUID id) {
        return ResponseEntity.ok(service.removePhoto(id));
    }

}

package br.com.stella.api.controller;

import br.com.munif.common.controller.SuperController;
import br.com.munif.common.dto.RevisionDTO;
import br.com.stella.api.dto.StorageLocationCreateDTO;
import br.com.stella.api.dto.StorageLocationResponseDTO;
import br.com.stella.api.dto.StorageLocationSummaryDTO;
import br.com.stella.api.dto.StorageLocationUpdateDTO;
import br.com.stella.api.entity.StorageLocation;
import br.com.stella.api.service.StorageLocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing storage locations.
 *
 * <p>Exposes the {@code /api/v0/locais} resource with full CRUD, name search,
 * representation image upload and removal, and audit revision queries.</p>
 *
 * <p>Locations can be organized in a parent-child hierarchy. The listing returns
 * nodes depth-first, with the full path and level of each node.</p>
 */
@RestController
@RequestMapping("/api/v0/locais")
public class StorageLocationController extends SuperController<StorageLocationSummaryDTO, StorageLocationResponseDTO, StorageLocationCreateDTO, StorageLocationUpdateDTO, StorageLocation> {

    private final StorageLocationService service;

    /**
     * Constructs the controller injecting the storage location service.
     *
     * @param service location business service
     */
    public StorageLocationController(StorageLocationService service) {
        this.service = service;
    }

    @Override
    @PostMapping
    public ResponseEntity<StorageLocationResponseDTO> create(@RequestBody @Valid StorageLocationCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<StorageLocationResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findResponseById(id));
    }

    @Override
    @GetMapping
    public ResponseEntity<List<StorageLocationSummaryDTO>> listar() {
        return ResponseEntity.ok(service.listSummary());
    }

    /**
     * Finds active locations whose name contains the given text (case-insensitive).
     * Returns results in hierarchical order.
     *
     * @param name substring to search in the location name
     * @return {@code 200 OK} with the list of found locations
     */
    @GetMapping("/search")
    public ResponseEntity<List<StorageLocationSummaryDTO>> findByName(@RequestParam String name) {
        return ResponseEntity.ok(service.findByName(name));
    }

    /**
     * Updates the representation image of a location via file upload.
     *
     * @param id      UUID of the location
     * @param arquivo image file uploaded by the client
     * @return {@code 200 OK} with the full DTO of the updated location
     */
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StorageLocationResponseDTO> updateImage(@PathVariable UUID id, @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(service.updateImage(id, arquivo));
    }

    /**
     * Removes the representation image of a location and deletes the file from MinIO.
     *
     * @param id UUID of the location
     * @return {@code 200 OK} with the full DTO of the location without an image
     */
    @DeleteMapping("/{id}/image")
    public ResponseEntity<StorageLocationResponseDTO> removerImagem(@PathVariable UUID id) {
        return ResponseEntity.ok(service.removerImagem(id));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<StorageLocationResponseDTO> update(@PathVariable UUID id, @RequestBody @Valid StorageLocationUpdateDTO dto) {
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
    public ResponseEntity<List<StorageLocationSummaryDTO>> findAllIncludingInactive() {
        return ResponseEntity.ok(service.listSummaryIncludingInactive());
    }

    @Override
    @GetMapping("/{id}/revisoes")
    public ResponseEntity<List<RevisionDTO<StorageLocation>>> listPreviousVersions(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listRevisions(id));
    }
}

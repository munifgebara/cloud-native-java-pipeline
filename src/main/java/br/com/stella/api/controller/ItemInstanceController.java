package br.com.stella.api.controller;

import br.com.munif.common.controller.SuperController;
import br.com.munif.common.dto.RevisionDTO;
import br.com.stella.api.dto.ItemInstanceCreateDTO;
import br.com.stella.api.dto.ItemInstanceHistoryDTO;
import br.com.stella.api.dto.ItemInstanceResponseDTO;
import br.com.stella.api.dto.ItemInstanceSummaryDTO;
import br.com.stella.api.dto.ItemInstanceUpdateDTO;
import br.com.stella.api.entity.ItemInstance;
import br.com.stella.api.entity.ItemInstanceStatus;
import br.com.stella.api.service.ItemInstanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing physical instances of inventory items.
 *
 * <p>Exposes the endpoints for the {@code /api/v0/instancias-item} resource, covering CRUD,
 * filters, movement history and audit revision queries.</p>
 *
 * <p>An instance is the physical occurrence of an {@link br.com.stella.api.entity.MainItem}.
 * For example: the main item "Notebook Dell" can have 10 registered instances,
 * each with its own asset tag, location and operational status.</p>
 */
@RestController
@RequestMapping("/api/v0/instancias-item")
public class ItemInstanceController extends SuperController<ItemInstanceSummaryDTO, ItemInstanceResponseDTO, ItemInstanceCreateDTO, ItemInstanceUpdateDTO, ItemInstance> {

    private final ItemInstanceService service;

    /**
     * Constructs the controller injecting the business service.
     *
     * @param service item instance service
     */
    public ItemInstanceController(ItemInstanceService service) {
        this.service = service;
    }

    @Override
    @PostMapping
    public ResponseEntity<ItemInstanceResponseDTO> create(@RequestBody @Valid ItemInstanceCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<ItemInstanceResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findResponseById(id));
    }

    /**
     * Returns the movement history of a specific instance.
     *
     * @param id UUID of the instance
     * @return {@code 200 OK} with the instance and its list of movements in chronological order
     */
    @GetMapping("/{id}/historico")
    public ResponseEntity<ItemInstanceHistoryDTO> buscarHistorico(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarHistorico(id));
    }

    @Override
    @GetMapping
    public ResponseEntity<List<ItemInstanceSummaryDTO>> listar() {
        return ResponseEntity.ok(service.listSummary());
    }

    /**
     * Finds active instances whose identifier contains the given text.
     *
     * @param identificador text to search in the {@code identificador} field
     * @return {@code 200 OK} with the list of found instances
     */
    @GetMapping("/search")
    public ResponseEntity<List<ItemInstanceSummaryDTO>> findByIdentifier(@RequestParam String identificador) {
        return ResponseEntity.ok(service.findByIdentifier(identificador));
    }

    /**
     * Filters active instances with multiple optional criteria.
     * Parameters not provided are ignored (no restriction on the corresponding field).
     *
     * @param identificacao   text to search in identifier, asset tag or serial number
     * @param mainItem      substring of the main item name
     * @param categoriaId     UUID of the category
     * @param statusOperacional desired operational status
     * @return {@code 200 OK} with the list of instances satisfying the criteria
     */
    @GetMapping("/filtrar")
    public ResponseEntity<List<ItemInstanceSummaryDTO>> filtrar(
            @RequestParam(required = false) String identificacao,
            @RequestParam(required = false) String mainItem,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) ItemInstanceStatus statusOperacional
    ) {
        return ResponseEntity.ok(service.filtrar(identificacao, mainItem, categoriaId, statusOperacional));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<ItemInstanceResponseDTO> update(@PathVariable UUID id, @RequestBody @Valid ItemInstanceUpdateDTO dto) {
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
    public ResponseEntity<List<ItemInstanceSummaryDTO>> findAllIncludingInactive() {
        return ResponseEntity.ok(service.listSummaryIncludingInactive());
    }

    @Override
    @GetMapping("/{id}/revisoes")
    public ResponseEntity<List<RevisionDTO<ItemInstance>>> listPreviousVersions(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listRevisions(id));
    }
}

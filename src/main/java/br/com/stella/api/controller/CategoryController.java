package br.com.stella.api.controller;

import br.com.munif.common.controller.SuperController;
import br.com.munif.common.dto.RevisionDTO;
import br.com.stella.api.dto.CategoryCreateDTO;
import br.com.stella.api.dto.CategoryResponseDTO;
import br.com.stella.api.dto.CategorySummaryDTO;
import br.com.stella.api.dto.CategoryUpdateDTO;
import br.com.stella.api.entity.Category;
import br.com.stella.api.service.CategoryService;
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
 * REST controller for managing item categories.
 *
 * <p>Exposes the {@code /api/v0/categories} resource with full CRUD,
 * name search, and audit history queries.</p>
 *
 * <p>Categories group main items by type (e.g.: "Electronics", "Furniture")
 * and are used in filtering and the dashboard.</p>
 */
@RestController
@RequestMapping("/api/v0/categories")
public class CategoryController extends SuperController<CategorySummaryDTO, CategoryResponseDTO, CategoryCreateDTO, CategoryUpdateDTO, Category> {

    private final CategoryService service;

    /**
     * Constructs the controller injecting the category service.
     *
     * @param service category business service
     */
    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @Override
    @PostMapping
    public ResponseEntity<CategoryResponseDTO> create(@RequestBody @Valid CategoryCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findResponseById(id));
    }

    @Override
    @GetMapping
    public ResponseEntity<List<CategorySummaryDTO>> list() {
        return ResponseEntity.ok(service.listSummary());
    }

    /**
     * Finds active categories whose name contains the given text (case-insensitive).
     *
     * @param name substring to search in the category name
     * @return {@code 200 OK} with the list of found categories
     */
    @GetMapping("/search")
    public ResponseEntity<List<CategorySummaryDTO>> findByName(@RequestParam String name) {
        return ResponseEntity.ok(service.findByName(name));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> update(@PathVariable UUID id, @RequestBody @Valid CategoryUpdateDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteLogically(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/all")
    public ResponseEntity<List<CategorySummaryDTO>> findAllIncludingInactive() {
        return ResponseEntity.ok(service.listSummaryIncludingInactive());
    }

    @Override
    @GetMapping("/{id}/revisions")
    public ResponseEntity<List<RevisionDTO<Category>>> listPreviousVersions(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listRevisions(id));
    }
}

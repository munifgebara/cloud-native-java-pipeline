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
 * <p>Exposes the {@code /api/v0/categorias} resource with full CRUD,
 * name search, and audit history queries.</p>
 *
 * <p>Categories group main items by type (e.g.: "Electronics", "Furniture")
 * and are used in filtering and the dashboard.</p>
 */
@RestController
@RequestMapping("/api/v0/categorias")
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
    public ResponseEntity<CategoryResponseDTO> criar(@RequestBody @Valid CategoryCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarResponsePorId(id));
    }

    @Override
    @GetMapping
    public ResponseEntity<List<CategorySummaryDTO>> listar() {
        return ResponseEntity.ok(service.listarResumo());
    }

    /**
     * Finds active categories whose name contains the given text (case-insensitive).
     *
     * @param nome substring to search in the category name
     * @return {@code 200 OK} with the list of found categories
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<CategorySummaryDTO>> buscarPorNome(@RequestParam String nome) {
        return ResponseEntity.ok(service.buscarPorNome(nome));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> atualizar(@PathVariable UUID id, @RequestBody @Valid CategoryUpdateDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluirLogicamente(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/todos")
    public ResponseEntity<List<CategorySummaryDTO>> findAllIncludingInactive() {
        return ResponseEntity.ok(service.listarResumoIncluindoInativos());
    }

    @Override
    @GetMapping("/{id}/revisoes")
    public ResponseEntity<List<RevisionDTO<Category>>> listarVersoesAnteriores(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listarRevisoes(id));
    }
}

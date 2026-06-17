package br.com.stella.api.controller;

import br.com.munif.common.controller.SuperController;
import br.com.munif.common.dto.RevisionDTO;
import br.com.stella.api.dto.SemanticSearchItemDTO;
import br.com.stella.api.dto.MainItemCreateDTO;
import br.com.stella.api.dto.ImageAiRequestDTO;
import br.com.stella.api.dto.ImageAiResponseDTO;
import br.com.stella.api.dto.MainItemResponseDTO;
import br.com.stella.api.dto.MainItemSummaryDTO;
import br.com.stella.api.dto.MainItemUpdateDTO;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.service.ImageAiService;
import br.com.stella.api.service.MainItemService;
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
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing inventory main items.
 *
 * <p>Exposes the {@code /api/v0/itens-mestre} resource with CRUD, filters, AI-powered semantic
 * search, main image upload and audit revision queries.</p>
 *
 * <p>A <em>main item</em> represents a model or type of asset (e.g.: "Notebook Dell Inspiron 15").
 * Individual physical units are represented by {@link br.com.stella.api.entity.ItemInstance}.</p>
 */
@RestController
@RequestMapping("/api/v0/itens-mestre")
public class MainItemController extends SuperController<MainItemSummaryDTO, MainItemResponseDTO, MainItemCreateDTO, MainItemUpdateDTO, MainItem> {

    private final MainItemService service;
    private final ImageAiService imageAiService;

    /**
     * Constructs the controller injecting the required services.
     *
     * @param service         main item business service
     * @param imageAiService AI image generation service
     */
    public MainItemController(MainItemService service, ImageAiService imageAiService) {
        this.service = service;
        this.imageAiService = imageAiService;
    }

    @Override
    @PostMapping
    public ResponseEntity<MainItemResponseDTO> create(@RequestBody @Valid MainItemCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<MainItemResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findResponseById(id));
    }

    @Override
    @GetMapping
    public ResponseEntity<List<MainItemSummaryDTO>> listar() {
        return ResponseEntity.ok(service.listSummary());
    }

    /**
     * Finds active main items whose name contains the given text (case-insensitive).
     *
     * @param name substring to search in the main item name
     * @return {@code 200 OK} with the list of found items
     */
    @GetMapping("/search")
    public ResponseEntity<List<MainItemSummaryDTO>> findByName(@RequestParam String name) {
        return ResponseEntity.ok(service.findByName(name));
    }

    /**
     * Filters active main items with multiple optional criteria.
     *
     * @param name        substring of the main item name; ignored if not provided
     * @param categoryId UUID of the category; ignored if not provided
     * @return {@code 200 OK} with the list of items satisfying the criteria
     */
    @GetMapping("/filtrar")
    public ResponseEntity<List<MainItemSummaryDTO>> filtrar(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) UUID categoryId) {
        return ResponseEntity.ok(service.filtrar(name, categoryId));
    }

    /**
     * Performs semantic search (vector similarity) on active main items.
     *
     * @param query free text to search semantically
     * @return {@code 200 OK} with the items most similar to the query, ordered by relevance
     */
    @GetMapping("/busca-semantic")
    public ResponseEntity<List<SemanticSearchItemDTO>> searchSemantically(@RequestParam("query") String query) {
        return ResponseEntity.ok(service.searchSemantically(query));
    }

    /**
     * Forces the vector re-indexing of all active main items.
     * Useful after bulk changes or index failures.
     *
     * @return {@code 200 OK} with the number of re-indexed items
     */
    @PostMapping("/busca-semantic/reindexar")
    public ResponseEntity<Map<String, Integer>> reindexSemanticSearch() {
        return ResponseEntity.ok(Map.of("itensReindexados", service.reindexSemanticSearch()));
    }

    /**
     * Updates the main image of a main item via file upload.
     *
     * @param id             UUID of the main item
     * @param file        image file uploaded by the client
     * @param generatedByAi  indicates whether the image was generated by AI
     * @param provider       name of the AI provider (optional, provided when {@code generatedByAi} is {@code true})
     * @return {@code 200 OK} with the full DTO of the updated item
     */
    @PostMapping(value = "/{id}/image-principal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MainItemResponseDTO> updateMainImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "generatedByAi", defaultValue = "false") boolean generatedByAi,
            @RequestParam(value = "provider", required = false) String provider
    ) {
        return ResponseEntity.ok(service.updateMainImage(id, file, generatedByAi, provider));
    }

    /**
     * Generates an image for a main item using artificial intelligence.
     *
     * @param dto input data with the item description and other parameters
     * @return {@code 200 OK} with the URL or data of the generated image
     */
    @PostMapping("/image-ia")
    public ResponseEntity<ImageAiResponseDTO> gerarImagemIa(@RequestBody @Valid ImageAiRequestDTO dto) {
        return ResponseEntity.ok(imageAiService.generateImage(dto));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<MainItemResponseDTO> update(@PathVariable UUID id, @RequestBody @Valid MainItemUpdateDTO dto) {
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
    public ResponseEntity<List<MainItemSummaryDTO>> findAllIncludingInactive() {
        return ResponseEntity.ok(service.listSummaryIncludingInactive());
    }

    @Override
    @GetMapping("/{id}/revisoes")
    public ResponseEntity<List<RevisionDTO<MainItem>>> listPreviousVersions(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listRevisions(id));
    }
}

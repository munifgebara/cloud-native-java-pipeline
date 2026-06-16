package br.com.munif.stella.api.controller;

import br.com.munif.comum.controller.SuperController;
import br.com.munif.comum.dto.RevisaoDTO;
import br.com.munif.stella.api.dto.ConsultaSemanticaItemDTO;
import br.com.munif.stella.api.dto.ItemMestreCreateDTO;
import br.com.munif.stella.api.dto.ImagemIaRequestDTO;
import br.com.munif.stella.api.dto.ImagemIaResponseDTO;
import br.com.munif.stella.api.dto.ItemMestreResponseDTO;
import br.com.munif.stella.api.dto.ItemMestreResumoDTO;
import br.com.munif.stella.api.dto.ItemMestreUpdateDTO;
import br.com.munif.stella.api.entity.ItemMestre;
import br.com.munif.stella.api.service.ImagemIaService;
import br.com.munif.stella.api.service.ItemMestreService;
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
 * Individual physical units are represented by {@link br.com.munif.stella.api.entity.InstanciaItem}.</p>
 */
@RestController
@RequestMapping("/api/v0/itens-mestre")
public class ItemMestreController extends SuperController<ItemMestreResumoDTO, ItemMestreResponseDTO, ItemMestreCreateDTO, ItemMestreUpdateDTO, ItemMestre> {

    private final ItemMestreService service;
    private final ImagemIaService imagemIaService;

    /**
     * Constructs the controller injecting the required services.
     *
     * @param service         main item business service
     * @param imagemIaService AI image generation service
     */
    public ItemMestreController(ItemMestreService service, ImagemIaService imagemIaService) {
        this.service = service;
        this.imagemIaService = imagemIaService;
    }

    @Override
    @PostMapping
    public ResponseEntity<ItemMestreResponseDTO> criar(@RequestBody @Valid ItemMestreCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<ItemMestreResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarResponsePorId(id));
    }

    @Override
    @GetMapping
    public ResponseEntity<List<ItemMestreResumoDTO>> listar() {
        return ResponseEntity.ok(service.listarResumo());
    }

    /**
     * Finds active main items whose name contains the given text (case-insensitive).
     *
     * @param nome substring to search in the main item name
     * @return {@code 200 OK} with the list of found items
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<ItemMestreResumoDTO>> buscarPorNome(@RequestParam String nome) {
        return ResponseEntity.ok(service.buscarPorNome(nome));
    }

    /**
     * Filters active main items with multiple optional criteria.
     *
     * @param nome        substring of the main item name; ignored if not provided
     * @param categoriaId UUID of the category; ignored if not provided
     * @return {@code 200 OK} with the list of items satisfying the criteria
     */
    @GetMapping("/filtrar")
    public ResponseEntity<List<ItemMestreResumoDTO>> filtrar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) UUID categoriaId) {
        return ResponseEntity.ok(service.filtrar(nome, categoriaId));
    }

    /**
     * Performs semantic search (vector similarity) on active main items.
     *
     * @param consulta free text to search semantically
     * @return {@code 200 OK} with the items most similar to the query, ordered by relevance
     */
    @GetMapping("/busca-semantica")
    public ResponseEntity<List<ConsultaSemanticaItemDTO>> buscarSemanticamente(@RequestParam("consulta") String consulta) {
        return ResponseEntity.ok(service.buscarSemanticamente(consulta));
    }

    /**
     * Forces the vector re-indexing of all active main items.
     * Useful after bulk changes or index failures.
     *
     * @return {@code 200 OK} with the number of re-indexed items
     */
    @PostMapping("/busca-semantica/reindexar")
    public ResponseEntity<Map<String, Integer>> reindexarBuscaSemantica() {
        return ResponseEntity.ok(Map.of("itensReindexados", service.reindexarBuscaSemantica()));
    }

    /**
     * Updates the main image of a main item via file upload.
     *
     * @param id             UUID of the main item
     * @param arquivo        image file uploaded by the client
     * @param generatedByAi  indicates whether the image was generated by AI
     * @param provider       name of the AI provider (optional, provided when {@code generatedByAi} is {@code true})
     * @return {@code 200 OK} with the full DTO of the updated item
     */
    @PostMapping(value = "/{id}/imagem-principal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemMestreResponseDTO> atualizarImagemPrincipal(
            @PathVariable UUID id,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "generatedByAi", defaultValue = "false") boolean generatedByAi,
            @RequestParam(value = "provider", required = false) String provider
    ) {
        return ResponseEntity.ok(service.atualizarImagemPrincipal(id, arquivo, generatedByAi, provider));
    }

    /**
     * Generates an image for a main item using artificial intelligence.
     *
     * @param dto input data with the item description and other parameters
     * @return {@code 200 OK} with the URL or data of the generated image
     */
    @PostMapping("/imagem-ia")
    public ResponseEntity<ImagemIaResponseDTO> gerarImagemIa(@RequestBody @Valid ImagemIaRequestDTO dto) {
        return ResponseEntity.ok(imagemIaService.gerarImagem(dto));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<ItemMestreResponseDTO> atualizar(@PathVariable UUID id, @RequestBody @Valid ItemMestreUpdateDTO dto) {
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
    public ResponseEntity<List<ItemMestreResumoDTO>> listarTodosIncluindoInativos() {
        return ResponseEntity.ok(service.listarResumoIncluindoInativos());
    }

    @Override
    @GetMapping("/{id}/revisoes")
    public ResponseEntity<List<RevisaoDTO<ItemMestre>>> listarVersoesAnteriores(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listarRevisoes(id));
    }
}

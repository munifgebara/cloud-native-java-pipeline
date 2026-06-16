package br.com.munif.stella.api.controller;

import br.com.munif.comum.controller.SuperController;
import br.com.munif.comum.dto.RevisaoDTO;
import br.com.munif.stella.api.dto.InstanciaItemCreateDTO;
import br.com.munif.stella.api.dto.InstanciaItemHistoricoDTO;
import br.com.munif.stella.api.dto.InstanciaItemResponseDTO;
import br.com.munif.stella.api.dto.InstanciaItemResumoDTO;
import br.com.munif.stella.api.dto.InstanciaItemUpdateDTO;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import br.com.munif.stella.api.service.InstanciaItemService;
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
 * <p>An instance is the physical occurrence of an {@link br.com.munif.stella.api.entity.ItemMestre}.
 * For example: the main item "Notebook Dell" can have 10 registered instances,
 * each with its own asset tag, location and operational status.</p>
 */
@RestController
@RequestMapping("/api/v0/instancias-item")
public class InstanciaItemController extends SuperController<InstanciaItemResumoDTO, InstanciaItemResponseDTO, InstanciaItemCreateDTO, InstanciaItemUpdateDTO, InstanciaItem> {

    private final InstanciaItemService service;

    /**
     * Constructs the controller injecting the business service.
     *
     * @param service item instance service
     */
    public InstanciaItemController(InstanciaItemService service) {
        this.service = service;
    }

    @Override
    @PostMapping
    public ResponseEntity<InstanciaItemResponseDTO> criar(@RequestBody @Valid InstanciaItemCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<InstanciaItemResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarResponsePorId(id));
    }

    /**
     * Returns the movement history of a specific instance.
     *
     * @param id UUID of the instance
     * @return {@code 200 OK} with the instance and its list of movements in chronological order
     */
    @GetMapping("/{id}/historico")
    public ResponseEntity<InstanciaItemHistoricoDTO> buscarHistorico(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarHistorico(id));
    }

    @Override
    @GetMapping
    public ResponseEntity<List<InstanciaItemResumoDTO>> listar() {
        return ResponseEntity.ok(service.listarResumo());
    }

    /**
     * Finds active instances whose identifier contains the given text.
     *
     * @param identificador text to search in the {@code identificador} field
     * @return {@code 200 OK} with the list of found instances
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<InstanciaItemResumoDTO>> buscarPorIdentificador(@RequestParam String identificador) {
        return ResponseEntity.ok(service.buscarPorIdentificador(identificador));
    }

    /**
     * Filters active instances with multiple optional criteria.
     * Parameters not provided are ignored (no restriction on the corresponding field).
     *
     * @param identificacao   text to search in identifier, asset tag or serial number
     * @param itemMestre      substring of the main item name
     * @param categoriaId     UUID of the category
     * @param statusOperacional desired operational status
     * @return {@code 200 OK} with the list of instances satisfying the criteria
     */
    @GetMapping("/filtrar")
    public ResponseEntity<List<InstanciaItemResumoDTO>> filtrar(
            @RequestParam(required = false) String identificacao,
            @RequestParam(required = false) String itemMestre,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) StatusOperacionalInstancia statusOperacional
    ) {
        return ResponseEntity.ok(service.filtrar(identificacao, itemMestre, categoriaId, statusOperacional));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<InstanciaItemResponseDTO> atualizar(@PathVariable UUID id, @RequestBody @Valid InstanciaItemUpdateDTO dto) {
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
    public ResponseEntity<List<InstanciaItemResumoDTO>> listarTodosIncluindoInativos() {
        return ResponseEntity.ok(service.listarResumoIncluindoInativos());
    }

    @Override
    @GetMapping("/{id}/revisoes")
    public ResponseEntity<List<RevisaoDTO<InstanciaItem>>> listarVersoesAnteriores(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listarRevisoes(id));
    }
}

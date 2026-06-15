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
 * Controller REST para gerenciamento de instâncias físicas de itens do inventário.
 *
 * <p>Expõe os endpoints do recurso {@code /api/v0/instancias-item}, cobrindo CRUD,
 * filtros, histórico de movimentações e consulta de revisões de auditoria.</p>
 *
 * <p>Uma instância é a ocorrência física de um {@link br.com.munif.stella.api.entity.ItemMestre}.
 * Por exemplo: o item mestre "Notebook Dell" pode ter 10 instâncias cadastradas,
 * cada uma com seu próprio patrimônio, local e status operacional.</p>
 */
@RestController
@RequestMapping("/api/v0/instancias-item")
public class InstanciaItemController extends SuperController<InstanciaItemResumoDTO, InstanciaItemResponseDTO, InstanciaItemCreateDTO, InstanciaItemUpdateDTO, InstanciaItem> {

    private final InstanciaItemService service;

    /**
     * Constrói o controller injetando o serviço de negócio.
     *
     * @param service serviço de instâncias de item
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
     * Retorna o histórico de movimentações de uma instância específica.
     *
     * @param id UUID da instância
     * @return {@code 200 OK} com a instância e sua lista de movimentações em ordem cronológica
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
     * Busca instâncias ativas cujo identificador contenha o texto informado.
     *
     * @param identificador texto a buscar no campo {@code identificador}
     * @return {@code 200 OK} com a lista de instâncias encontradas
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<InstanciaItemResumoDTO>> buscarPorIdentificador(@RequestParam String identificador) {
        return ResponseEntity.ok(service.buscarPorIdentificador(identificador));
    }

    /**
     * Filtra instâncias ativas com múltiplos critérios opcionais.
     * Parâmetros não informados são ignorados (sem restrição sobre o campo correspondente).
     *
     * @param identificacao   texto a buscar em identificador, patrimônio ou número de série
     * @param itemMestre      substring do nome do item mestre
     * @param categoriaId     UUID da categoria
     * @param statusOperacional status operacional desejado
     * @return {@code 200 OK} com a lista de instâncias que satisfazem os critérios
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

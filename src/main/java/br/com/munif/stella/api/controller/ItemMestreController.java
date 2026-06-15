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
 * Controller REST para gerenciamento de itens mestres do inventário.
 *
 * <p>Expõe o recurso {@code /api/v0/itens-mestre} com CRUD, filtros, busca semântica via IA,
 * upload de imagem principal e consulta de revisões de auditoria.</p>
 *
 * <p>Um <em>item mestre</em> representa um modelo ou tipo de bem (ex.: "Notebook Dell Inspiron 15").
 * As unidades físicas individuais são representadas por {@link br.com.munif.stella.api.entity.InstanciaItem}.</p>
 */
@RestController
@RequestMapping("/api/v0/itens-mestre")
public class ItemMestreController extends SuperController<ItemMestreResumoDTO, ItemMestreResponseDTO, ItemMestreCreateDTO, ItemMestreUpdateDTO, ItemMestre> {

    private final ItemMestreService service;
    private final ImagemIaService imagemIaService;

    /**
     * Constrói o controller injetando os serviços necessários.
     *
     * @param service       serviço de negócio de itens mestres
     * @param imagemIaService serviço de geração de imagens via IA
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
     * Busca itens mestres ativos cujo nome contenha o texto informado (case-insensitive).
     *
     * @param nome substring a buscar no nome do item mestre
     * @return {@code 200 OK} com a lista de itens encontrados
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<ItemMestreResumoDTO>> buscarPorNome(@RequestParam String nome) {
        return ResponseEntity.ok(service.buscarPorNome(nome));
    }

    /**
     * Filtra itens mestres ativos com múltiplos critérios opcionais.
     *
     * @param nome        substring do nome do item mestre; ignorado se não informado
     * @param categoriaId UUID da categoria; ignorado se não informado
     * @return {@code 200 OK} com a lista de itens que satisfazem os critérios
     */
    @GetMapping("/filtrar")
    public ResponseEntity<List<ItemMestreResumoDTO>> filtrar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) UUID categoriaId) {
        return ResponseEntity.ok(service.filtrar(nome, categoriaId));
    }

    /**
     * Realiza busca semântica (similaridade de vetor) nos itens mestres ativos.
     *
     * @param consulta texto livre a buscar semanticamente
     * @return {@code 200 OK} com os itens mais similares à consulta, ordenados por relevância
     */
    @GetMapping("/busca-semantica")
    public ResponseEntity<List<ConsultaSemanticaItemDTO>> buscarSemanticamente(@RequestParam("consulta") String consulta) {
        return ResponseEntity.ok(service.buscarSemanticamente(consulta));
    }

    /**
     * Força a reindexação vetorial de todos os itens mestres ativos.
     * Útil após alterações em massa ou falhas no índice.
     *
     * @return {@code 200 OK} com o número de itens reindexados
     */
    @PostMapping("/busca-semantica/reindexar")
    public ResponseEntity<Map<String, Integer>> reindexarBuscaSemantica() {
        return ResponseEntity.ok(Map.of("itensReindexados", service.reindexarBuscaSemantica()));
    }

    /**
     * Atualiza a imagem principal de um item mestre via upload de arquivo.
     *
     * @param id             UUID do item mestre
     * @param arquivo        arquivo de imagem enviado pelo cliente
     * @param generatedByAi  indica se a imagem foi gerada por IA
     * @param provider       nome do provedor de IA (opcional, informado quando {@code generatedByAi} for {@code true})
     * @return {@code 200 OK} com o DTO completo do item atualizado
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
     * Gera uma imagem para um item mestre usando inteligência artificial.
     *
     * @param dto dados de entrada com a descrição do item e demais parâmetros
     * @return {@code 200 OK} com a URL ou dados da imagem gerada
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

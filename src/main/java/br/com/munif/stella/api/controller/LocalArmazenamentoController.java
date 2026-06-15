package br.com.munif.stella.api.controller;

import br.com.munif.comum.controller.SuperController;
import br.com.munif.comum.dto.RevisaoDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoCreateDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoResponseDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoResumoDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoUpdateDTO;
import br.com.munif.stella.api.entity.LocalArmazenamento;
import br.com.munif.stella.api.service.LocalArmazenamentoService;
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
 * Controller REST para gerenciamento de locais de armazenamento.
 *
 * <p>Expõe o recurso {@code /api/v0/locais} com CRUD completo, busca por nome,
 * upload e remoção de imagem de representação, e consulta de revisões de auditoria.</p>
 *
 * <p>Locais podem ser organizados em hierarquia pai-filho. A listagem retorna os
 * nós em profundidade primeiro, com caminho completo e nível de cada nó.</p>
 */
@RestController
@RequestMapping("/api/v0/locais")
public class LocalArmazenamentoController extends SuperController<LocalArmazenamentoResumoDTO, LocalArmazenamentoResponseDTO, LocalArmazenamentoCreateDTO, LocalArmazenamentoUpdateDTO, LocalArmazenamento> {

    private final LocalArmazenamentoService service;

    /**
     * Constrói o controller injetando o serviço de locais de armazenamento.
     *
     * @param service serviço de negócio de locais
     */
    public LocalArmazenamentoController(LocalArmazenamentoService service) {
        this.service = service;
    }

    @Override
    @PostMapping
    public ResponseEntity<LocalArmazenamentoResponseDTO> criar(@RequestBody @Valid LocalArmazenamentoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<LocalArmazenamentoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarResponsePorId(id));
    }

    @Override
    @GetMapping
    public ResponseEntity<List<LocalArmazenamentoResumoDTO>> listar() {
        return ResponseEntity.ok(service.listarResumo());
    }

    /**
     * Busca locais ativos cujo nome contenha o texto informado (case-insensitive).
     * Retorna os resultados em ordem hierárquica.
     *
     * @param nome substring a buscar no nome do local
     * @return {@code 200 OK} com a lista de locais encontrados
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<LocalArmazenamentoResumoDTO>> buscarPorNome(@RequestParam String nome) {
        return ResponseEntity.ok(service.buscarPorNome(nome));
    }

    /**
     * Atualiza a imagem de representação de um local via upload de arquivo.
     *
     * @param id      UUID do local
     * @param arquivo arquivo de imagem enviado pelo cliente
     * @return {@code 200 OK} com o DTO completo do local atualizado
     */
    @PostMapping(value = "/{id}/imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LocalArmazenamentoResponseDTO> atualizarImagem(@PathVariable UUID id, @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(service.atualizarImagem(id, arquivo));
    }

    /**
     * Remove a imagem de representação de um local e exclui o arquivo do MinIO.
     *
     * @param id UUID do local
     * @return {@code 200 OK} com o DTO completo do local sem imagem
     */
    @DeleteMapping("/{id}/imagem")
    public ResponseEntity<LocalArmazenamentoResponseDTO> removerImagem(@PathVariable UUID id) {
        return ResponseEntity.ok(service.removerImagem(id));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<LocalArmazenamentoResponseDTO> atualizar(@PathVariable UUID id, @RequestBody @Valid LocalArmazenamentoUpdateDTO dto) {
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
    public ResponseEntity<List<LocalArmazenamentoResumoDTO>> listarTodosIncluindoInativos() {
        return ResponseEntity.ok(service.listarResumoIncluindoInativos());
    }

    @Override
    @GetMapping("/{id}/revisoes")
    public ResponseEntity<List<RevisaoDTO<LocalArmazenamento>>> listarVersoesAnteriores(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listarRevisoes(id));
    }
}

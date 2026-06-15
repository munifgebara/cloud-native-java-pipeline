package br.com.munif.stella.api.controller;

import br.com.munif.comum.controller.SuperController;
import br.com.munif.stella.api.dto.PessoaCreateDTO;
import br.com.munif.stella.api.dto.PessoaResponseDTO;
import br.com.munif.stella.api.dto.PessoaRevisaoDTO;
import br.com.munif.stella.api.dto.PessoaResumoDTO;
import br.com.munif.stella.api.dto.PessoaUpdateDTO;
import br.com.munif.stella.api.entity.Pessoa;
import br.com.munif.stella.api.service.PessoaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para gerenciamento de pessoas (físicas ou jurídicas).
 *
 * <p>Expõe o recurso {@code /api/v0/pessoas} com CRUD, busca por nome
 * e histórico de revisões de auditoria.</p>
 */
@RestController
@RequestMapping("/api/v0/pessoas")
public class PessoaController extends SuperController<PessoaResumoDTO, PessoaResponseDTO, PessoaCreateDTO, PessoaUpdateDTO, Pessoa> {

    private final PessoaService service;

    /**
     * Constrói o controller injetando o serviço de negócio de pessoas.
     *
     * @param service serviço de pessoas
     */
    public PessoaController(PessoaService service) {
        this.service = service;
    }

    @Override
    @PostMapping
    public ResponseEntity<PessoaResponseDTO> criar(@RequestBody @Valid PessoaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<PessoaResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarResponsePorId(id));
    }

    @Override
    @GetMapping
    public ResponseEntity<List<PessoaResumoDTO>> listar() {
        return ResponseEntity.ok(service.listarResumo());
    }

    /**
     * Busca pessoas ativas cujo nome contenha o texto informado (busca parcial, case-insensitive).
     *
     * @param nome substring a buscar no nome da pessoa
     * @return {@code 200 OK} com a lista de pessoas encontradas
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<PessoaResumoDTO>> buscarPorNome(@RequestParam String nome) {
        return ResponseEntity.ok(service.buscarPorNome(nome));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<PessoaResponseDTO> atualizar(@PathVariable UUID id, @RequestBody @Valid PessoaUpdateDTO dto) {
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
    public ResponseEntity<List<PessoaResumoDTO>> listarTodosIncluindoInativos() {
        return ResponseEntity.ok(service.listarResumoIncluindoInativos());
    }

    @Override
    @GetMapping("/{id}/revisoes")
    public ResponseEntity<List<PessoaRevisaoDTO>> listarVersoesAnteriores(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listarRevisoes(id));
    }
}

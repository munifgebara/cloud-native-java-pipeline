package br.com.munif.pagadoria.api.controller;

import br.com.munif.comum.controller.SuperController;
import br.com.munif.comum.dto.RevisaoDTO;
import br.com.munif.pagadoria.api.dto.PessoaCreateDTO;
import br.com.munif.pagadoria.api.dto.PessoaResponseDTO;
import br.com.munif.pagadoria.api.dto.PessoaResumoDTO;
import br.com.munif.pagadoria.api.dto.PessoaUpdateDTO;
import br.com.munif.pagadoria.api.entity.Pessoa;
import br.com.munif.pagadoria.api.service.PessoaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pessoas")
public class PessoaController extends SuperController<PessoaResumoDTO, PessoaResponseDTO, PessoaCreateDTO, PessoaUpdateDTO, Pessoa> {

    private final PessoaService service;

    public PessoaController(PessoaService service) {
        this.service = service;
    }

    @Override
    @PostMapping
    public ResponseEntity<PessoaResponseDTO> criar(@RequestBody PessoaCreateDTO dto) {
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

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<PessoaResponseDTO> atualizar(@PathVariable UUID id, @RequestBody PessoaUpdateDTO dto) {
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
    public ResponseEntity<List<RevisaoDTO<Pessoa>>> listarVersoesAnteriores(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listarRevisoes(id));
    }
}

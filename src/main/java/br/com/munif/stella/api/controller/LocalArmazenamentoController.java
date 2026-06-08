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

@RestController
@RequestMapping("/api/v0/locais")
public class LocalArmazenamentoController extends SuperController<LocalArmazenamentoResumoDTO, LocalArmazenamentoResponseDTO, LocalArmazenamentoCreateDTO, LocalArmazenamentoUpdateDTO, LocalArmazenamento> {

    private final LocalArmazenamentoService service;

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

    @GetMapping("/buscar")
    public ResponseEntity<List<LocalArmazenamentoResumoDTO>> buscarPorNome(@RequestParam String nome) {
        return ResponseEntity.ok(service.buscarPorNome(nome));
    }

    @PostMapping(value = "/{id}/imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LocalArmazenamentoResponseDTO> atualizarImagem(@PathVariable UUID id, @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(service.atualizarImagem(id, arquivo));
    }

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

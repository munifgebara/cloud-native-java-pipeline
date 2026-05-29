package br.com.munif.stella.api.controller;

import br.com.munif.comum.controller.SuperController;
import br.com.munif.comum.dto.RevisaoDTO;
import br.com.munif.stella.api.dto.CategoriaCreateDTO;
import br.com.munif.stella.api.dto.CategoriaResponseDTO;
import br.com.munif.stella.api.dto.CategoriaResumoDTO;
import br.com.munif.stella.api.dto.CategoriaUpdateDTO;
import br.com.munif.stella.api.entity.Categoria;
import br.com.munif.stella.api.service.CategoriaService;
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

@RestController
@RequestMapping("/api/v0/categorias")
public class CategoriaController extends SuperController<CategoriaResumoDTO, CategoriaResponseDTO, CategoriaCreateDTO, CategoriaUpdateDTO, Categoria> {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @Override
    @PostMapping
    public ResponseEntity<CategoriaResponseDTO> criar(@RequestBody @Valid CategoriaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarResponsePorId(id));
    }

    @Override
    @GetMapping
    public ResponseEntity<List<CategoriaResumoDTO>> listar() {
        return ResponseEntity.ok(service.listarResumo());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<CategoriaResumoDTO>> buscarPorNome(@RequestParam String nome) {
        return ResponseEntity.ok(service.buscarPorNome(nome));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> atualizar(@PathVariable UUID id, @RequestBody @Valid CategoriaUpdateDTO dto) {
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
    public ResponseEntity<List<CategoriaResumoDTO>> listarTodosIncluindoInativos() {
        return ResponseEntity.ok(service.listarResumoIncluindoInativos());
    }

    @Override
    @GetMapping("/{id}/revisoes")
    public ResponseEntity<List<RevisaoDTO<Categoria>>> listarVersoesAnteriores(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listarRevisoes(id));
    }
}

package br.com.munif.stella.api.controller;

import br.com.munif.comum.controller.SuperController;
import br.com.munif.comum.dto.RevisaoDTO;
import br.com.munif.stella.api.dto.ItemMestreCreateDTO;
import br.com.munif.stella.api.dto.ItemMestreResponseDTO;
import br.com.munif.stella.api.dto.ItemMestreResumoDTO;
import br.com.munif.stella.api.dto.ItemMestreUpdateDTO;
import br.com.munif.stella.api.entity.ItemMestre;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/itens-mestre")
public class ItemMestreController extends SuperController<ItemMestreResumoDTO, ItemMestreResponseDTO, ItemMestreCreateDTO, ItemMestreUpdateDTO, ItemMestre> {

    private final ItemMestreService service;

    public ItemMestreController(ItemMestreService service) {
        this.service = service;
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

    @GetMapping("/buscar")
    public ResponseEntity<List<ItemMestreResumoDTO>> buscarPorNome(@RequestParam String nome) {
        return ResponseEntity.ok(service.buscarPorNome(nome));
    }

    @GetMapping("/filtrar")
    public ResponseEntity<List<ItemMestreResumoDTO>> filtrar(@RequestParam(required = false) String nome, @RequestParam(required = false) UUID categoriaId) {
        return ResponseEntity.ok(service.filtrar(nome, categoriaId));
    }

    @PostMapping(value = "/{id}/imagem-principal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemMestreResponseDTO> atualizarImagemPrincipal(@PathVariable UUID id, @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(service.atualizarImagemPrincipal(id, arquivo));
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

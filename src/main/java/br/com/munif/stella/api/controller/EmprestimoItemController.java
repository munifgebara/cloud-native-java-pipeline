package br.com.munif.stella.api.controller;

import br.com.munif.stella.api.dto.EmprestimoItemCreateDTO;
import br.com.munif.stella.api.dto.EmprestimoItemDevolucaoDTO;
import br.com.munif.stella.api.dto.EmprestimoItemResponseDTO;
import br.com.munif.stella.api.service.EmprestimoItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v0/emprestimos-item")
public class EmprestimoItemController {

    private final EmprestimoItemService service;

    public EmprestimoItemController(EmprestimoItemService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<EmprestimoItemResponseDTO> registrarEmprestimo(@RequestBody @Valid EmprestimoItemCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarEmprestimo(dto));
    }

    @PostMapping("/devolucao")
    public ResponseEntity<EmprestimoItemResponseDTO> registrarDevolucao(@RequestBody @Valid EmprestimoItemDevolucaoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarDevolucao(dto));
    }
}

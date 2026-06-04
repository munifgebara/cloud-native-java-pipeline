package br.com.munif.stella.api.controller;

import br.com.munif.stella.api.dto.MovimentacaoEntradaCreateDTO;
import br.com.munif.stella.api.dto.MovimentacaoItemResponseDTO;
import br.com.munif.stella.api.dto.MovimentacaoSaidaCreateDTO;
import br.com.munif.stella.api.dto.MovimentacaoTransferenciaCreateDTO;
import br.com.munif.stella.api.service.MovimentacaoItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v0/movimentacoes-item")
public class MovimentacaoItemController {

    private final MovimentacaoItemService service;

    public MovimentacaoItemController(MovimentacaoItemService service) {
        this.service = service;
    }

    @PostMapping("/entrada")
    public ResponseEntity<MovimentacaoItemResponseDTO> registrarEntrada(@RequestBody @Valid MovimentacaoEntradaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarEntrada(dto));
    }

    @PostMapping("/saida")
    public ResponseEntity<MovimentacaoItemResponseDTO> registrarSaida(@RequestBody @Valid MovimentacaoSaidaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarSaida(dto));
    }

    @PostMapping("/transferencia")
    public ResponseEntity<MovimentacaoItemResponseDTO> registrarTransferencia(@RequestBody @Valid MovimentacaoTransferenciaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarTransferencia(dto));
    }
}

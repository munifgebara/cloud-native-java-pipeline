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

/**
 * REST controller for recording item instance movements.
 *
 * <p>Exposes the {@code /api/v0/movimentacoes-item} resource with three operations:</p>
 * <ul>
 *   <li><strong>Entry</strong> — first association of an instance to a location (initial registration).</li>
 *   <li><strong>Exit</strong> — removal of an instance from the active inventory.</li>
 *   <li><strong>Transfer</strong> — movement of an instance between two locations.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v0/movimentacoes-item")
public class MovimentacaoItemController {

    private final MovimentacaoItemService service;

    /**
     * Constructs the controller injecting the movement service.
     *
     * @param service service responsible for the movement business rules
     */
    public MovimentacaoItemController(MovimentacaoItemService service) {
        this.service = service;
    }

    /**
     * Records the entry of an instance into a storage location.
     *
     * @param dto entry data validated by Bean Validation
     * @return {@code 201 Created} with the recorded movement data
     */
    @PostMapping("/entrada")
    public ResponseEntity<MovimentacaoItemResponseDTO> registrarEntrada(@RequestBody @Valid MovimentacaoEntradaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarEntrada(dto));
    }

    /**
     * Records the exit of an instance from the inventory.
     *
     * @param dto exit data validated by Bean Validation
     * @return {@code 201 Created} with the recorded movement data
     */
    @PostMapping("/saida")
    public ResponseEntity<MovimentacaoItemResponseDTO> registrarSaida(@RequestBody @Valid MovimentacaoSaidaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarSaida(dto));
    }

    /**
     * Records the transfer of an instance from one location to another.
     *
     * @param dto transfer data validated by Bean Validation
     * @return {@code 201 Created} with the recorded movement data
     */
    @PostMapping("/transferencia")
    public ResponseEntity<MovimentacaoItemResponseDTO> registrarTransferencia(@RequestBody @Valid MovimentacaoTransferenciaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarTransferencia(dto));
    }
}

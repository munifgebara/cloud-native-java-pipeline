package br.com.stella.api.controller;

import br.com.stella.api.dto.ItemInputMovementCreateDTO;
import br.com.stella.api.dto.ItemMovementResponseDTO;
import br.com.stella.api.dto.ItemOutputMovementCreateDTO;
import br.com.stella.api.dto.ItemTransferMovementCreateDTO;
import br.com.stella.api.service.ItemMovementService;
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
 * <p>Exposes the {@code /api/v0/movements-item} resource with three operations:</p>
 * <ul>
 *   <li><strong>Entry</strong> — first association of an instance to a location (initial registration).</li>
 *   <li><strong>Exit</strong> — removal of an instance from the active inventory.</li>
 *   <li><strong>Transfer</strong> — movement of an instance between two locations.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v0/movements-item")
public class ItemMovementController {

    private final ItemMovementService service;

    /**
     * Constructs the controller injecting the movement service.
     *
     * @param service service responsible for the movement business rules
     */
    public ItemMovementController(ItemMovementService service) {
        this.service = service;
    }

    /**
     * Records the entry of an instance into a storage location.
     *
     * @param dto entry data validated by Bean Validation
     * @return {@code 201 Created} with the recorded movement data
     */
    @PostMapping("/inbound")
    public ResponseEntity<ItemMovementResponseDTO> registerInbound(@RequestBody @Valid ItemInputMovementCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registerInbound(dto));
    }

    /**
     * Records the exit of an instance from the inventory.
     *
     * @param dto exit data validated by Bean Validation
     * @return {@code 201 Created} with the recorded movement data
     */
    @PostMapping("/outbound")
    public ResponseEntity<ItemMovementResponseDTO> registerOutbound(@RequestBody @Valid ItemOutputMovementCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registerOutbound(dto));
    }

    /**
     * Records the transfer of an instance from one location to another.
     *
     * @param dto transfer data validated by Bean Validation
     * @return {@code 201 Created} with the recorded movement data
     */
    @PostMapping("/transfer")
    public ResponseEntity<ItemMovementResponseDTO> registerTransfer(@RequestBody @Valid ItemTransferMovementCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registerTransfer(dto));
    }
}

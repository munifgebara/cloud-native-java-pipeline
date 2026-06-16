package br.com.stella.api.controller;

import br.com.stella.api.dto.ItemLoanCreateDTO;
import br.com.stella.api.dto.ItemLoanReturnDTO;
import br.com.stella.api.dto.ItemLoanResponseDTO;
import br.com.stella.api.service.ItemLoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for recording item instance loans and returns.
 *
 * <p>Exposes the {@code /api/v0/emprestimos-item} resource with two operations:</p>
 * <ul>
 *   <li><strong>Loan</strong> — associates an available instance with a person.</li>
 *   <li><strong>Return</strong> — closes the loan and returns the instance to the inventory.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v0/emprestimos-item")
public class ItemLoanController {

    private final ItemLoanService service;

    /**
     * Constructs the controller injecting the loan service.
     *
     * @param service loan business service
     */
    public ItemLoanController(ItemLoanService service) {
        this.service = service;
    }

    /**
     * Records the loan of an instance to a person.
     *
     * @param dto loan data validated by Bean Validation
     * @return {@code 201 Created} with the recorded loan data
     */
    @PostMapping
    public ResponseEntity<ItemLoanResponseDTO> registrarEmprestimo(@RequestBody @Valid ItemLoanCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarEmprestimo(dto));
    }

    /**
     * Records the return of a loaned instance.
     *
     * @param dto return data validated by Bean Validation
     * @return {@code 201 Created} with the closed loan data
     */
    @PostMapping("/devolucao")
    public ResponseEntity<ItemLoanResponseDTO> registrarDevolucao(@RequestBody @Valid ItemLoanReturnDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarDevolucao(dto));
    }
}

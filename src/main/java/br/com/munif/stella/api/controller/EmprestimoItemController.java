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
public class EmprestimoItemController {

    private final EmprestimoItemService service;

    /**
     * Constructs the controller injecting the loan service.
     *
     * @param service loan business service
     */
    public EmprestimoItemController(EmprestimoItemService service) {
        this.service = service;
    }

    /**
     * Records the loan of an instance to a person.
     *
     * @param dto loan data validated by Bean Validation
     * @return {@code 201 Created} with the recorded loan data
     */
    @PostMapping
    public ResponseEntity<EmprestimoItemResponseDTO> registrarEmprestimo(@RequestBody @Valid EmprestimoItemCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarEmprestimo(dto));
    }

    /**
     * Records the return of a loaned instance.
     *
     * @param dto return data validated by Bean Validation
     * @return {@code 201 Created} with the closed loan data
     */
    @PostMapping("/devolucao")
    public ResponseEntity<EmprestimoItemResponseDTO> registrarDevolucao(@RequestBody @Valid EmprestimoItemDevolucaoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarDevolucao(dto));
    }
}

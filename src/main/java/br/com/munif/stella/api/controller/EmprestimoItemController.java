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
 * Controller REST para registro de empréstimos e devoluções de instâncias de itens.
 *
 * <p>Expõe o recurso {@code /api/v0/emprestimos-item} com duas operações:</p>
 * <ul>
 *   <li><strong>Empréstimo</strong> — associa uma instância disponível a uma pessoa.</li>
 *   <li><strong>Devolução</strong> — encerra o empréstimo e retorna a instância ao inventário.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v0/emprestimos-item")
public class EmprestimoItemController {

    private final EmprestimoItemService service;

    /**
     * Constrói o controller injetando o serviço de empréstimos.
     *
     * @param service serviço de negócio de empréstimos
     */
    public EmprestimoItemController(EmprestimoItemService service) {
        this.service = service;
    }

    /**
     * Registra o empréstimo de uma instância a uma pessoa.
     *
     * @param dto dados do empréstimo validados pelo Bean Validation
     * @return {@code 201 Created} com os dados do empréstimo registrado
     */
    @PostMapping
    public ResponseEntity<EmprestimoItemResponseDTO> registrarEmprestimo(@RequestBody @Valid EmprestimoItemCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarEmprestimo(dto));
    }

    /**
     * Registra a devolução de uma instância emprestada.
     *
     * @param dto dados da devolução validados pelo Bean Validation
     * @return {@code 201 Created} com os dados do empréstimo encerrado
     */
    @PostMapping("/devolucao")
    public ResponseEntity<EmprestimoItemResponseDTO> registrarDevolucao(@RequestBody @Valid EmprestimoItemDevolucaoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarDevolucao(dto));
    }
}

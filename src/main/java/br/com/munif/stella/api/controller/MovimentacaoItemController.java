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
 * Controller REST para registro de movimentações de instâncias de itens.
 *
 * <p>Expõe o recurso {@code /api/v0/movimentacoes-item} com três operações:</p>
 * <ul>
 *   <li><strong>Entrada</strong> — primeira associação de uma instância a um local (registro inicial).</li>
 *   <li><strong>Saída</strong> — retirada de uma instância do inventário ativo.</li>
 *   <li><strong>Transferência</strong> — movimentação de uma instância entre dois locais.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v0/movimentacoes-item")
public class MovimentacaoItemController {

    private final MovimentacaoItemService service;

    /**
     * Constrói o controller injetando o serviço de movimentações.
     *
     * @param service serviço responsável pelas regras de negócio de movimentação
     */
    public MovimentacaoItemController(MovimentacaoItemService service) {
        this.service = service;
    }

    /**
     * Registra a entrada de uma instância em um local de armazenamento.
     *
     * @param dto dados da entrada validados pelo Bean Validation
     * @return {@code 201 Created} com os dados da movimentação registrada
     */
    @PostMapping("/entrada")
    public ResponseEntity<MovimentacaoItemResponseDTO> registrarEntrada(@RequestBody @Valid MovimentacaoEntradaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarEntrada(dto));
    }

    /**
     * Registra a saída de uma instância do inventário.
     *
     * @param dto dados da saída validados pelo Bean Validation
     * @return {@code 201 Created} com os dados da movimentação registrada
     */
    @PostMapping("/saida")
    public ResponseEntity<MovimentacaoItemResponseDTO> registrarSaida(@RequestBody @Valid MovimentacaoSaidaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarSaida(dto));
    }

    /**
     * Registra a transferência de uma instância de um local para outro.
     *
     * @param dto dados da transferência validados pelo Bean Validation
     * @return {@code 201 Created} com os dados da movimentação registrada
     */
    @PostMapping("/transferencia")
    public ResponseEntity<MovimentacaoItemResponseDTO> registrarTransferencia(@RequestBody @Valid MovimentacaoTransferenciaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarTransferencia(dto));
    }
}

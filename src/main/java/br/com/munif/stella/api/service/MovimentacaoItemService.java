package br.com.munif.stella.api.service;

import br.com.munif.comum.service.SuperService;
import br.com.munif.comum.utils.validacoes.ValidacoesBR;
import br.com.munif.stella.api.dto.MovimentacaoEntradaCreateDTO;
import br.com.munif.stella.api.dto.MovimentacaoItemResponseDTO;
import br.com.munif.stella.api.dto.MovimentacaoSaidaCreateDTO;
import br.com.munif.stella.api.dto.MovimentacaoTransferenciaCreateDTO;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.ItemMestre;
import br.com.munif.stella.api.entity.LocalArmazenamento;
import br.com.munif.stella.api.entity.MovimentacaoItem;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import br.com.munif.stella.api.entity.TipoMovimentacaoItem;
import br.com.munif.stella.api.mapper.MovimentacaoItemMapper;
import br.com.munif.stella.api.repository.InstanciaItemRepository;
import br.com.munif.stella.api.repository.ItemMestreRepository;
import br.com.munif.stella.api.repository.LocalArmazenamentoRepository;
import br.com.munif.stella.api.repository.MovimentacaoItemRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Serviço responsável pelo registro de movimentações de instâncias de itens.
 *
 * <p>Implementa as três operações de movimentação do inventário:</p>
 * <ul>
 *   <li><strong>Entrada</strong> — cria a instância física e a associa a um local inicial.</li>
 *   <li><strong>Saída</strong> — retira a instância do inventário ativo, desvinculando-a do local.</li>
 *   <li><strong>Transferência</strong> — move a instância de um local para outro.</li>
 * </ul>
 *
 * <p>Cada operação valida o estado atual da instância por meio de {@link InstanciaItemRegras}
 * antes de persistir o registro de movimentação.</p>
 */
@Service
public class MovimentacaoItemService extends SuperService<MovimentacaoItem, MovimentacaoItemRepository> {

    private final InstanciaItemRepository instanciaItemRepository;
    private final ItemMestreRepository itemMestreRepository;
    private final LocalArmazenamentoRepository localArmazenamentoRepository;

    public MovimentacaoItemService(
            MovimentacaoItemRepository repository,
            EntityManager entityManager,
            InstanciaItemRepository instanciaItemRepository,
            ItemMestreRepository itemMestreRepository,
            LocalArmazenamentoRepository localArmazenamentoRepository
    ) {
        super(repository, entityManager, MovimentacaoItem.class);
        this.instanciaItemRepository = instanciaItemRepository;
        this.itemMestreRepository = itemMestreRepository;
        this.localArmazenamentoRepository = localArmazenamentoRepository;
    }

    /**
     * Registra a entrada de um novo bem no inventário, criando a instância e
     * associando-a ao local de destino informado com status {@code DISPONIVEL}.
     *
     * @param dto dados de entrada validados pelo Bean Validation
     * @return DTO da movimentação de entrada registrada
     * @throws IllegalArgumentException se o item mestre ou local não existirem, estiverem inativos,
     *                                  ou se nenhum identificador for informado
     */
    @Transactional
    public MovimentacaoItemResponseDTO registrarEntrada(MovimentacaoEntradaCreateDTO dto) {
        InstanciaItem instancia = new InstanciaItem();
        instancia.setItemMestre(buscarItemMestreAtivo(dto.itemMestreId()));
        instancia.setLocalAtual(buscarLocalAtivo(dto.localDestinoId()));
        instancia.setIdentificador(ValidacoesBR.trimToNull(dto.identificador()));
        instancia.setPatrimonio(ValidacoesBR.trimToNull(dto.patrimonio()));
        instancia.setNumeroSerie(ValidacoesBR.trimToNull(dto.numeroSerie()));
        instancia.setObservacoes(ValidacoesBR.trimToNull(dto.observacao()));
        instancia.setStatusOperacional(StatusOperacionalInstancia.DISPONIVEL);

        validarIdentificacao(instancia);

        InstanciaItem instanciaSalva = instanciaItemRepository.save(instancia);

        MovimentacaoItem movimentacao = new MovimentacaoItem();
        movimentacao.setTipo(TipoMovimentacaoItem.ENTRADA);
        movimentacao.setInstanciaItem(instanciaSalva);
        movimentacao.setLocalDestino(instanciaSalva.getLocalAtual());
        movimentacao.setObservacao(ValidacoesBR.trimToNull(dto.observacao()));

        return MovimentacaoItemMapper.toResponseDTO(salvar(movimentacao));
    }

    /**
     * Registra a saída de uma instância do inventário.
     * A instância é desvinculada do local e tem status alterado para {@code EM_MOVIMENTACAO}.
     *
     * @param dto dados da saída validados pelo Bean Validation
     * @return DTO da movimentação de saída registrada
     * @throws IllegalArgumentException se a instância não existir, não estiver disponível,
     *                                  não tiver local atual, ou se o motivo for omitido
     */
    @Transactional
    public MovimentacaoItemResponseDTO registrarSaida(MovimentacaoSaidaCreateDTO dto) {
        InstanciaItem instancia = instanciaItemRepository.findById(dto.instanciaItemId())
                .orElseThrow(() -> new IllegalArgumentException("Instance not found."));
        InstanciaItemRegras.exigirDisponivelComLocal(
                instancia,
                "Instance must be active to register an outbound.",
                "Only available instances can register an outbound.",
                "Instance must have a current location to register an outbound."
        );

        LocalArmazenamento localOrigem = instancia.getLocalAtual();
        String motivo = ValidacoesBR.trimToNull(dto.motivo());
        if (motivo == null) {
            throw new IllegalArgumentException("Reason is required.");
        }

        instancia.setLocalAtual(null);
        instancia.setStatusOperacional(StatusOperacionalInstancia.EM_MOVIMENTACAO);
        instanciaItemRepository.save(instancia);

        MovimentacaoItem movimentacao = new MovimentacaoItem();
        movimentacao.setTipo(TipoMovimentacaoItem.SAIDA);
        movimentacao.setInstanciaItem(instancia);
        movimentacao.setLocalOrigem(localOrigem);
        movimentacao.setMotivo(motivo);
        movimentacao.setObservacao(ValidacoesBR.trimToNull(dto.observacao()));

        return MovimentacaoItemMapper.toResponseDTO(salvar(movimentacao));
    }

    /**
     * Registra a transferência de uma instância do local atual para um local de destino diferente.
     * O status da instância permanece {@code DISPONIVEL} após a transferência.
     *
     * @param dto dados da transferência validados pelo Bean Validation
     * @return DTO da movimentação de transferência registrada
     * @throws IllegalArgumentException se a instância não existir, não estiver disponível,
     *                                  se o local de destino não existir ou estiver inativo, ou
     *                                  se o local de destino for igual ao local atual
     */
    @Transactional
    public MovimentacaoItemResponseDTO registrarTransferencia(MovimentacaoTransferenciaCreateDTO dto) {
        InstanciaItem instancia = instanciaItemRepository.findById(dto.instanciaItemId())
                .orElseThrow(() -> new IllegalArgumentException("Instance not found."));
        InstanciaItemRegras.exigirDisponivelComLocal(
                instancia,
                "Instance must be active to register a transfer.",
                "Only available instances can be transferred.",
                "Instance must have a current location to register a transfer."
        );

        LocalArmazenamento localOrigem = instancia.getLocalAtual();
        LocalArmazenamento localDestino = buscarLocalAtivo(dto.localDestinoId());
        if (localOrigem.getId().equals(localDestino.getId())) {
            throw new IllegalArgumentException("Destination location must be different from the current location.");
        }

        instancia.setLocalAtual(localDestino);
        instancia.setStatusOperacional(StatusOperacionalInstancia.DISPONIVEL);
        instanciaItemRepository.save(instancia);

        MovimentacaoItem movimentacao = new MovimentacaoItem();
        movimentacao.setTipo(TipoMovimentacaoItem.TRANSFERENCIA);
        movimentacao.setInstanciaItem(instancia);
        movimentacao.setLocalOrigem(localOrigem);
        movimentacao.setLocalDestino(localDestino);
        movimentacao.setObservacao(ValidacoesBR.trimToNull(dto.observacao()));

        return MovimentacaoItemMapper.toResponseDTO(salvar(movimentacao));
    }

    private ItemMestre buscarItemMestreAtivo(UUID itemMestreId) {
        ItemMestre itemMestre = itemMestreRepository.findById(itemMestreId)
                .orElseThrow(() -> new IllegalArgumentException("Main item not found."));
        if (!itemMestre.isAtivo()) {
            throw new IllegalArgumentException("Main item must be active.");
        }
        return itemMestre;
    }

    private LocalArmazenamento buscarLocalAtivo(UUID localId) {
        LocalArmazenamento local = localArmazenamentoRepository.findById(localId)
                .orElseThrow(() -> new IllegalArgumentException("Destination location not found."));
        if (!local.isAtivo()) {
            throw new IllegalArgumentException("Destination location must be active.");
        }
        return local;
    }

    private void validarIdentificacao(InstanciaItem instancia) {
        if (instancia.getIdentificador() == null && instancia.getPatrimonio() == null && instancia.getNumeroSerie() == null) {
            throw new IllegalArgumentException("Provide identifier, asset number or serial number for the instance.");
        }
    }
}

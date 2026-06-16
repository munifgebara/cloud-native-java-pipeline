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
 * Service responsible for registering item instance movements.
 *
 * <p>Implements the three inventory movement operations:</p>
 * <ul>
 *   <li><strong>Inbound (Entrada)</strong> — creates the physical instance and associates it with an initial location.</li>
 *   <li><strong>Outbound (Saida)</strong> — removes the instance from the active inventory, unlinking it from the location.</li>
 *   <li><strong>Transfer (Transferencia)</strong> — moves the instance from one location to another.</li>
 * </ul>
 *
 * <p>Each operation validates the current state of the instance via {@link InstanciaItemRegras}
 * before persisting the movement record.</p>
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
     * Registers the inbound of a new asset in the inventory, creating the instance and
     * associating it with the provided destination location with status {@code DISPONIVEL}.
     *
     * @param dto inbound data validated by Bean Validation
     * @return DTO of the registered inbound movement
     * @throws IllegalArgumentException if the main item or location do not exist, are inactive,
     *                                  or if no identifier is provided
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
     * Registers the outbound of an instance from the inventory.
     * The instance is unlinked from the location and its status is changed to {@code EM_MOVIMENTACAO}.
     *
     * @param dto outbound data validated by Bean Validation
     * @return DTO of the registered outbound movement
     * @throws IllegalArgumentException if the instance does not exist, is not available,
     *                                  has no current location, or if the reason is omitted
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
     * Registers the transfer of an instance from the current location to a different destination location.
     * The instance status remains {@code DISPONIVEL} after the transfer.
     *
     * @param dto transfer data validated by Bean Validation
     * @return DTO of the registered transfer movement
     * @throws IllegalArgumentException if the instance does not exist, is not available,
     *                                  the destination location does not exist or is inactive, or
     *                                  the destination location is the same as the current location
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

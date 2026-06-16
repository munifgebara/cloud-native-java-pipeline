package br.com.munif.stella.api.service;

import br.com.munif.comum.dto.RevisaoDTO;
import br.com.munif.comum.service.SuperService;
import br.com.munif.comum.utils.validacoes.ValidacoesBR;
import br.com.munif.stella.api.dto.InstanciaItemCreateDTO;
import br.com.munif.stella.api.dto.InstanciaItemHistoricoDTO;
import br.com.munif.stella.api.dto.InstanciaItemResponseDTO;
import br.com.munif.stella.api.dto.InstanciaItemResumoDTO;
import br.com.munif.stella.api.dto.InstanciaItemUpdateDTO;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.ItemMestre;
import br.com.munif.stella.api.entity.LocalArmazenamento;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import br.com.munif.stella.api.mapper.InstanciaItemMapper;
import br.com.munif.stella.api.mapper.MovimentacaoItemMapper;
import br.com.munif.stella.api.observability.StructuredBusinessLogger;
import br.com.munif.stella.api.repository.EmprestimoItemRepository;
import br.com.munif.stella.api.repository.InstanciaItemRepository;
import br.com.munif.stella.api.repository.ItemMestreRepository;
import br.com.munif.stella.api.repository.LocalArmazenamentoRepository;
import br.com.munif.stella.api.repository.MovimentacaoItemRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service responsible for business operations on {@link InstanciaItem}.
 *
 * <p>Manages the lifecycle of physical inventory item instances:
 * creation via inbound, location and status update, logical deletion, and
 * movement history queries.</p>
 *
 * <p>Business rules on consistency between status and location are
 * delegated to the {@link InstanciaItemRegras} class.</p>
 */
@Service
public class InstanciaItemService extends SuperService<InstanciaItem, InstanciaItemRepository> {

    private static final Logger log = LoggerFactory.getLogger(InstanciaItemService.class);

    private final ItemMestreRepository itemMestreRepository;
    private final LocalArmazenamentoRepository localArmazenamentoRepository;
    private final MovimentacaoItemRepository movimentacaoItemRepository;
    private final EmprestimoItemRepository emprestimoItemRepository;

    public InstanciaItemService(
            InstanciaItemRepository repository,
            EntityManager entityManager,
            ItemMestreRepository itemMestreRepository,
            LocalArmazenamentoRepository localArmazenamentoRepository,
            MovimentacaoItemRepository movimentacaoItemRepository,
            EmprestimoItemRepository emprestimoItemRepository
    ) {
        super(repository, entityManager, InstanciaItem.class);
        this.itemMestreRepository = itemMestreRepository;
        this.localArmazenamentoRepository = localArmazenamentoRepository;
        this.movimentacaoItemRepository = movimentacaoItemRepository;
        this.emprestimoItemRepository = emprestimoItemRepository;
    }

    /**
     * Creates a new item instance in the inventory.
     *
     * @param dto creation data validated by Bean Validation
     * @return full DTO of the created instance
     * @throws IllegalArgumentException if the main item or location do not exist, are inactive,
     *                                  no identifier is provided, or the status and
     *                                  the location are incompatible
     */
    @Transactional
    public InstanciaItemResponseDTO criar(InstanciaItemCreateDTO dto) {
        InstanciaItem instancia = InstanciaItemMapper.toEntity(dto);
        normalizarCampos(instancia);
        validarIdentificacao(instancia);
        instancia.setItemMestre(buscarItemMestreAtivo(dto.itemMestreId()));
        instancia.setLocalAtual(buscarLocalAtivo(dto.localAtualId()));
        InstanciaItemRegras.validarCoerenciaStatusLocal(instancia);

        InstanciaItem salva = salvar(instancia);
        if (Boolean.FALSE.equals(dto.ativa())) {
            repository.flush();
            salva.setAtivo(false);
            salva = salvar(salva);
        }
        StructuredBusinessLogger.info(log, "inventory", "instance-created", StructuredBusinessLogger.fields(
                "instance_id", salva.getId(),
                "item_id", salva.getItemMestre() == null ? null : salva.getItemMestre().getId(),
                "location_id", salva.getLocalAtual() == null ? null : salva.getLocalAtual().getId(),
                "success", true
        ));
        return InstanciaItemMapper.toResponseDTO(salva);
    }

    /**
     * Returns the full DTO of an instance by its identifier.
     *
     * @param id UUID of the instance
     * @return full DTO of the instance
     * @throws jakarta.persistence.EntityNotFoundException if the instance does not exist
     */
    @Transactional(readOnly = true)
    public InstanciaItemResponseDTO buscarResponsePorId(UUID id) {
        return InstanciaItemMapper.toResponseDTO(buscarPorId(id));
    }

    /**
     * Returns the full movement history of an instance.
     *
     * @param id UUID of the instance
     * @return DTO with the instance and its list of movements in chronological order
     */
    @Transactional(readOnly = true)
    public InstanciaItemHistoricoDTO buscarHistorico(UUID id) {
        InstanciaItem instancia = buscarPorId(id);
        var movimentacoes = movimentacaoItemRepository.findByInstanciaItemIdOrderByDataMovimentacaoAscCriadoEmAsc(id).stream()
                .map(MovimentacaoItemMapper::toResponseDTO)
                .toList();

        return new InstanciaItemHistoricoDTO(
                InstanciaItemMapper.toResponseDTO(instancia),
                movimentacoes
        );
    }

    /**
     * Lists all active instances ordered by identifier, asset number, and serial number.
     *
     * @return list of summary DTOs of active instances
     */
    @Transactional(readOnly = true)
    public List<InstanciaItemResumoDTO> listarResumo() {
        return repository.findByAtivoTrueOrderByIdentificadorAscPatrimonioAscNumeroSerieAsc().stream()
                .map(InstanciaItemMapper::toResumoDTO)
                .toList();
    }

    /**
     * Lists all instances, including logically deactivated ones.
     *
     * @return list of summary DTOs of all instances
     */
    @Transactional(readOnly = true)
    public List<InstanciaItemResumoDTO> listarResumoIncluindoInativos() {
        return listarTodosIncluindoInativos().stream()
                .map(InstanciaItemMapper::toResumoDTO)
                .toList();
    }

    /**
     * Finds active instances whose {@code identificador} field contains the given text.
     *
     * @param identificador text to search; returns empty list if blank
     * @return list of summary DTOs ordered by identifier
     */
    @Transactional(readOnly = true)
    public List<InstanciaItemResumoDTO> buscarPorIdentificador(String identificador) {
        String valor = ValidacoesBR.trimToNull(identificador);
        if (valor == null) {
            return List.of();
        }

        return repository.findByAtivoTrueAndIdentificadorContainingIgnoreCaseOrderByIdentificadorAsc(valor).stream()
                .map(InstanciaItemMapper::toResumoDTO)
                .toList();
    }

    /**
     * Filters active instances combining multiple optional criteria.
     * Null or blank parameters are ignored.
     *
     * @param identificacao     text to search in identifier, asset number or serial number
     * @param itemMestre        substring of the main item name
     * @param categoriaId       UUID of the main item category
     * @param statusOperacional desired operational status
     * @return list of DTOs ordered by identifier, asset number and serial number
     */
    @Transactional(readOnly = true)
    public List<InstanciaItemResumoDTO> filtrar(String identificacao, String itemMestre, UUID categoriaId, StatusOperacionalInstancia statusOperacional) {
        return repository.findAll(
                        InstanciaItemRepository.filtrarAtivas(
                                ValidacoesBR.trimToNull(identificacao),
                                ValidacoesBR.trimToNull(itemMestre),
                                categoriaId,
                                statusOperacional
                        ),
                        Sort.by(Sort.Order.asc("identificador").nullsLast(), Sort.Order.asc("patrimonio").nullsLast(), Sort.Order.asc("numeroSerie").nullsLast())
                ).stream()
                .map(InstanciaItemMapper::toResumoDTO)
                .toList();
    }

    /**
     * Updates the data of an existing instance.
     *
     * @param id  UUID of the instance to update
     * @param dto update data validated by Bean Validation
     * @return full DTO of the updated instance
     * @throws jakarta.persistence.EntityNotFoundException if the instance does not exist
     * @throws IllegalArgumentException if the main item or location are invalid,
     *                                  no identifier is provided, or the status and
     *                                  the location are incompatible
     */
    @Transactional
    public InstanciaItemResponseDTO atualizar(UUID id, InstanciaItemUpdateDTO dto) {
        InstanciaItem instancia = buscarPorId(id);
        UUID localAnteriorId = instancia.getLocalAtual() == null ? null : instancia.getLocalAtual().getId();
        ItemMestre itemMestre = buscarItemMestreAtivo(dto.itemMestreId());

        InstanciaItemMapper.updateEntity(instancia, dto);
        normalizarCampos(instancia);
        validarIdentificacao(instancia);
        instancia.setItemMestre(itemMestre);
        instancia.setLocalAtual(buscarLocalAtivo(dto.localAtualId()));
        InstanciaItemRegras.validarCoerenciaStatusLocal(instancia);

        InstanciaItem salva = salvar(instancia);
        UUID localAtualId = salva.getLocalAtual() == null ? null : salva.getLocalAtual().getId();
        String action = Objects.equals(localAnteriorId, localAtualId) ? "instance-updated" : "instance-location-updated";
        StructuredBusinessLogger.info(log, "inventory", action, StructuredBusinessLogger.fields(
                "instance_id", salva.getId(),
                "item_id", salva.getItemMestre() == null ? null : salva.getItemMestre().getId(),
                "previous_location_id", localAnteriorId,
                "location_id", localAtualId,
                "success", true
        ));
        return InstanciaItemMapper.toResponseDTO(salva);
    }

    @Transactional
    public void excluirLogicamente(UUID id) {
        if (movimentacaoItemRepository.existsByInstanciaItemId(id) || emprestimoItemRepository.existsByInstanciaItemId(id)) {
            throw new IllegalArgumentException("Instance with operational history cannot be deleted. Use the outbound operation to remove it from inventory.");
        }
        InstanciaItem instancia = buscarPorId(id);
        excluir(id);
        StructuredBusinessLogger.info(log, "inventory", "instance-deactivated", StructuredBusinessLogger.fields(
                "instance_id", id,
                "item_id", instancia.getItemMestre() == null ? null : instancia.getItemMestre().getId(),
                "location_id", instancia.getLocalAtual() == null ? null : instancia.getLocalAtual().getId(),
                "success", true
        ));
    }

    @Transactional(readOnly = true)
    public List<RevisaoDTO<InstanciaItem>> listarRevisoes(UUID id) {
        return listarVersoesAnteriores(id);
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
        if (localId == null) {
            return null;
        }

        LocalArmazenamento local = localArmazenamentoRepository.findById(localId)
                .orElseThrow(() -> new IllegalArgumentException("Current location not found."));
        if (!local.isAtivo()) {
            throw new IllegalArgumentException("Current location must be active.");
        }
        return local;
    }

    private void normalizarCampos(InstanciaItem instancia) {
        instancia.setIdentificador(ValidacoesBR.trimToNull(instancia.getIdentificador()));
        instancia.setPatrimonio(ValidacoesBR.trimToNull(instancia.getPatrimonio()));
        instancia.setNumeroSerie(ValidacoesBR.trimToNull(instancia.getNumeroSerie()));
        instancia.setObservacoes(ValidacoesBR.trimToNull(instancia.getObservacoes()));
    }

    private void validarIdentificacao(InstanciaItem instancia) {
        if (instancia.getIdentificador() == null && instancia.getPatrimonio() == null && instancia.getNumeroSerie() == null) {
            throw new IllegalArgumentException("Provide identifier, asset number or serial number for the instance.");
        }
    }
}

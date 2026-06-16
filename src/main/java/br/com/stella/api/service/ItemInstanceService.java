package br.com.stella.api.service;

import br.com.munif.common.dto.RevisionDTO;
import br.com.munif.common.service.SuperService;
import br.com.munif.common.utils.validacoes.BrValidations;
import br.com.stella.api.dto.ItemInstanceCreateDTO;
import br.com.stella.api.dto.ItemInstanceHistoryDTO;
import br.com.stella.api.dto.ItemInstanceResponseDTO;
import br.com.stella.api.dto.ItemInstanceSummaryDTO;
import br.com.stella.api.dto.ItemInstanceUpdateDTO;
import br.com.stella.api.entity.ItemInstance;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.entity.StorageLocation;
import br.com.stella.api.entity.ItemInstanceStatus;
import br.com.stella.api.mapper.InstanciaItemMapper;
import br.com.stella.api.mapper.ItemMovementMapper;
import br.com.stella.api.observability.StructuredBusinessLogger;
import br.com.stella.api.repository.ItemLoanRepository;
import br.com.stella.api.repository.ItemInstanceRepository;
import br.com.stella.api.repository.MainItemRepository;
import br.com.stella.api.repository.StorageLocationRepository;
import br.com.stella.api.repository.ItemMovementRepository;
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
 * Service responsible for business operations on {@link ItemInstance}.
 *
 * <p>Manages the lifecycle of physical inventory item instances:
 * creation via inbound, location and status update, logical deletion, and
 * movement history queries.</p>
 *
 * <p>Business rules on consistency between status and location are
 * delegated to the {@link ItemInstanceRules} class.</p>
 */
@Service
public class ItemInstanceService extends SuperService<ItemInstance, ItemInstanceRepository> {

    private static final Logger log = LoggerFactory.getLogger(ItemInstanceService.class);

    private final MainItemRepository itemMestreRepository;
    private final StorageLocationRepository localArmazenamentoRepository;
    private final ItemMovementRepository movimentacaoItemRepository;
    private final ItemLoanRepository emprestimoItemRepository;

    public ItemInstanceService(
            ItemInstanceRepository repository,
            EntityManager entityManager,
            MainItemRepository itemMestreRepository,
            StorageLocationRepository localArmazenamentoRepository,
            ItemMovementRepository movimentacaoItemRepository,
            ItemLoanRepository emprestimoItemRepository
    ) {
        super(repository, entityManager, ItemInstance.class);
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
    public ItemInstanceResponseDTO criar(ItemInstanceCreateDTO dto) {
        ItemInstance instance = InstanciaItemMapper.toEntity(dto);
        normalizarCampos(instance);
        validarIdentificacao(instance);
        instance.setMainItem(buscarItemMestreAtivo(dto.itemMestreId()));
        instance.setCurrentLocation(buscarLocalAtivo(dto.localAtualId()));
        ItemInstanceRules.validarCoerenciaStatusLocal(instance);

        ItemInstance salva = salvar(instance);
        if (Boolean.FALSE.equals(dto.ativa())) {
            repository.flush();
            salva.setActive(false);
            salva = salvar(salva);
        }
        StructuredBusinessLogger.info(log, "inventory", "instance-created", StructuredBusinessLogger.fields(
                "instance_id", salva.getId(),
                "item_id", salva.getMainItem() == null ? null : salva.getMainItem().getId(),
                "location_id", salva.getCurrentLocation() == null ? null : salva.getCurrentLocation().getId(),
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
    public ItemInstanceResponseDTO buscarResponsePorId(UUID id) {
        return InstanciaItemMapper.toResponseDTO(buscarPorId(id));
    }

    /**
     * Returns the full movement history of an instance.
     *
     * @param id UUID of the instance
     * @return DTO with the instance and its list of movements in chronological order
     */
    @Transactional(readOnly = true)
    public ItemInstanceHistoryDTO buscarHistorico(UUID id) {
        ItemInstance instance = buscarPorId(id);
        var movimentacoes = movimentacaoItemRepository.findByInstanciaItemIdOrderByDataMovimentacaoAscCriadoEmAsc(id).stream()
                .map(ItemMovementMapper::toResponseDTO)
                .toList();

        return new ItemInstanceHistoryDTO(
                InstanciaItemMapper.toResponseDTO(instance),
                movimentacoes
        );
    }

    /**
     * Lists all active instances ordered by identifier, asset number, and serial number.
     *
     * @return list of summary DTOs of active instances
     */
    @Transactional(readOnly = true)
    public List<ItemInstanceSummaryDTO> listarResumo() {
        return repository.findByActiveTrueOrderByIdentifierAscAssetTagAscSerialNumberAsc().stream()
                .map(InstanciaItemMapper::toResumoDTO)
                .toList();
    }

    /**
     * Lists all instances, including logically deactivated ones.
     *
     * @return list of summary DTOs of all instances
     */
    @Transactional(readOnly = true)
    public List<ItemInstanceSummaryDTO> listarResumoIncluindoInativos() {
        return findAllIncludingInactive().stream()
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
    public List<ItemInstanceSummaryDTO> buscarPorIdentificador(String identificador) {
        String valor = BrValidations.trimToNull(identificador);
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
     * @param mainItem        substring of the main item name
     * @param categoriaId       UUID of the main item category
     * @param statusOperacional desired operational status
     * @return list of DTOs ordered by identifier, asset number and serial number
     */
    @Transactional(readOnly = true)
    public List<ItemInstanceSummaryDTO> filtrar(String identificacao, String mainItem, UUID categoriaId, ItemInstanceStatus statusOperacional) {
        return repository.findAll(
                        ItemInstanceRepository.filtrarAtivas(
                                BrValidations.trimToNull(identificacao),
                                BrValidations.trimToNull(mainItem),
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
    public ItemInstanceResponseDTO atualizar(UUID id, ItemInstanceUpdateDTO dto) {
        ItemInstance instance = buscarPorId(id);
        UUID localAnteriorId = instance.getCurrentLocation() == null ? null : instance.getCurrentLocation().getId();
        MainItem mainItem = buscarItemMestreAtivo(dto.itemMestreId());

        InstanciaItemMapper.updateEntity(instance, dto);
        normalizarCampos(instance);
        validarIdentificacao(instance);
        instance.setMainItem(mainItem);
        instance.setCurrentLocation(buscarLocalAtivo(dto.localAtualId()));
        ItemInstanceRules.validarCoerenciaStatusLocal(instance);

        ItemInstance salva = salvar(instance);
        UUID localAtualId = salva.getCurrentLocation() == null ? null : salva.getCurrentLocation().getId();
        String action = Objects.equals(localAnteriorId, localAtualId) ? "instance-updated" : "instance-location-updated";
        StructuredBusinessLogger.info(log, "inventory", action, StructuredBusinessLogger.fields(
                "instance_id", salva.getId(),
                "item_id", salva.getMainItem() == null ? null : salva.getMainItem().getId(),
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
        ItemInstance instance = buscarPorId(id);
        excluir(id);
        StructuredBusinessLogger.info(log, "inventory", "instance-deactivated", StructuredBusinessLogger.fields(
                "instance_id", id,
                "item_id", instance.getMainItem() == null ? null : instance.getMainItem().getId(),
                "location_id", instance.getCurrentLocation() == null ? null : instance.getCurrentLocation().getId(),
                "success", true
        ));
    }

    @Transactional(readOnly = true)
    public List<RevisionDTO<ItemInstance>> listarRevisoes(UUID id) {
        return listarVersoesAnteriores(id);
    }

    private MainItem buscarItemMestreAtivo(UUID itemMestreId) {
        MainItem mainItem = itemMestreRepository.findById(itemMestreId)
                .orElseThrow(() -> new IllegalArgumentException("Main item not found."));
        if (!mainItem.isActive()) {
            throw new IllegalArgumentException("Main item must be active.");
        }
        return mainItem;
    }

    private StorageLocation buscarLocalAtivo(UUID localId) {
        if (localId == null) {
            return null;
        }

        StorageLocation location = localArmazenamentoRepository.findById(localId)
                .orElseThrow(() -> new IllegalArgumentException("Current location not found."));
        if (!location.isActive()) {
            throw new IllegalArgumentException("Current location must be active.");
        }
        return location;
    }

    private void normalizarCampos(ItemInstance instance) {
        instance.setIdentifier(BrValidations.trimToNull(instance.getIdentifier()));
        instance.setAssetTag(BrValidations.trimToNull(instance.getAssetTag()));
        instance.setSerialNumber(BrValidations.trimToNull(instance.getSerialNumber()));
        instance.setNotes(BrValidations.trimToNull(instance.getNotes()));
    }

    private void validarIdentificacao(ItemInstance instance) {
        if (instance.getIdentifier() == null && instance.getAssetTag() == null && instance.getSerialNumber() == null) {
            throw new IllegalArgumentException("Provide identifier, asset number or serial number for the instance.");
        }
    }
}

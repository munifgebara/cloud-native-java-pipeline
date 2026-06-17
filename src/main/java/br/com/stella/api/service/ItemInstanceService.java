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
import br.com.stella.api.mapper.ItemInstanceMapper;
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

    private final MainItemRepository mainItemRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final ItemMovementRepository itemMovementRepository;
    private final ItemLoanRepository itemLoanRepository;

    public ItemInstanceService(
            ItemInstanceRepository repository,
            EntityManager entityManager,
            MainItemRepository mainItemRepository,
            StorageLocationRepository storageLocationRepository,
            ItemMovementRepository itemMovementRepository,
            ItemLoanRepository itemLoanRepository
    ) {
        super(repository, entityManager, ItemInstance.class);
        this.mainItemRepository = mainItemRepository;
        this.storageLocationRepository = storageLocationRepository;
        this.itemMovementRepository = itemMovementRepository;
        this.itemLoanRepository = itemLoanRepository;
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
    public ItemInstanceResponseDTO create(ItemInstanceCreateDTO dto) {
        ItemInstance instance = ItemInstanceMapper.toEntity(dto);
        normalizarCampos(instance);
        validateIdentification(instance);
        instance.setMainItem(findActiveMainItem(dto.mainItemId()));
        instance.setCurrentLocation(findActiveLocation(dto.currentLocationId()));
        ItemInstanceRules.validateStatusLocationConsistency(instance);

        ItemInstance salva = save(instance);
        if (Boolean.FALSE.equals(dto.ativa())) {
            repository.flush();
            salva.setActive(false);
            salva = save(salva);
        }
        StructuredBusinessLogger.info(log, "inventory", "instance-created", StructuredBusinessLogger.fields(
                "instance_id", salva.getId(),
                "item_id", salva.getMainItem() == null ? null : salva.getMainItem().getId(),
                "location_id", salva.getCurrentLocation() == null ? null : salva.getCurrentLocation().getId(),
                "success", true
        ));
        return ItemInstanceMapper.toResponseDTO(salva);
    }

    /**
     * Returns the full DTO of an instance by its identifier.
     *
     * @param id UUID of the instance
     * @return full DTO of the instance
     * @throws jakarta.persistence.EntityNotFoundException if the instance does not exist
     */
    @Transactional(readOnly = true)
    public ItemInstanceResponseDTO findResponseById(UUID id) {
        return ItemInstanceMapper.toResponseDTO(findById(id));
    }

    /**
     * Returns the full movement history of an instance.
     *
     * @param id UUID of the instance
     * @return DTO with the instance and its list of movements in chronological order
     */
    @Transactional(readOnly = true)
    public ItemInstanceHistoryDTO findHistory(UUID id) {
        ItemInstance instance = findById(id);
        var movimentacoes = itemMovementRepository.findByItemInstanceIdOrderByMovementDateAscCreatedAtAsc(id).stream()
                .map(ItemMovementMapper::toResponseDTO)
                .toList();

        return new ItemInstanceHistoryDTO(
                ItemInstanceMapper.toResponseDTO(instance),
                movimentacoes
        );
    }

    /**
     * Lists all active instances ordered by identifier, asset number, and serial number.
     *
     * @return list of summary DTOs of active instances
     */
    @Transactional(readOnly = true)
    public List<ItemInstanceSummaryDTO> listSummary() {
        return repository.findByActiveTrueOrderByIdentifierAscAssetTagAscSerialNumberAsc().stream()
                .map(ItemInstanceMapper::toResumoDTO)
                .toList();
    }

    /**
     * Lists all instances, including logically deactivated ones.
     *
     * @return list of summary DTOs of all instances
     */
    @Transactional(readOnly = true)
    public List<ItemInstanceSummaryDTO> listSummaryIncludingInactive() {
        return findAllIncludingInactive().stream()
                .map(ItemInstanceMapper::toResumoDTO)
                .toList();
    }

    /**
     * Finds active instances whose {@code identifier} field contains the given text.
     *
     * @param identifier text to search; returns empty list if blank
     * @return list of summary DTOs ordered by identifier
     */
    @Transactional(readOnly = true)
    public List<ItemInstanceSummaryDTO> findByIdentifier(String identifier) {
        String valor = BrValidations.trimToNull(identifier);
        if (valor == null) {
            return List.of();
        }

        return repository.findByActiveTrueAndIdentifierContainingIgnoreCaseOrderByIdentifierAsc(valor).stream()
                .map(ItemInstanceMapper::toResumoDTO)
                .toList();
    }

    /**
     * Filters active instances combining multiple optional criteria.
     * Null or blank parameters are ignored.
     *
     * @param identification     text to search in identifier, asset number or serial number
     * @param mainItem        substring of the main item name
     * @param categoryId       UUID of the main item category
     * @param operationalStatus desired operational status
     * @return list of DTOs ordered by identifier, asset number and serial number
     */
    @Transactional(readOnly = true)
    public List<ItemInstanceSummaryDTO> filtrar(String identification, String mainItem, UUID categoryId, ItemInstanceStatus operationalStatus) {
        return repository.findAll(
                        ItemInstanceRepository.filterActive(
                                BrValidations.trimToNull(identification),
                                BrValidations.trimToNull(mainItem),
                                categoryId,
                                operationalStatus
                        ),
                        Sort.by(Sort.Order.asc("identifier").nullsLast(), Sort.Order.asc("assetTag").nullsLast(), Sort.Order.asc("serialNumber").nullsLast())
                ).stream()
                .map(ItemInstanceMapper::toResumoDTO)
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
    public ItemInstanceResponseDTO update(UUID id, ItemInstanceUpdateDTO dto) {
        ItemInstance instance = findById(id);
        UUID previousLocationId = instance.getCurrentLocation() == null ? null : instance.getCurrentLocation().getId();
        MainItem mainItem = findActiveMainItem(dto.mainItemId());

        ItemInstanceMapper.updateEntity(instance, dto);
        normalizarCampos(instance);
        validateIdentification(instance);
        instance.setMainItem(mainItem);
        instance.setCurrentLocation(findActiveLocation(dto.currentLocationId()));
        ItemInstanceRules.validateStatusLocationConsistency(instance);

        ItemInstance salva = save(instance);
        UUID currentLocationId = salva.getCurrentLocation() == null ? null : salva.getCurrentLocation().getId();
        String action = Objects.equals(previousLocationId, currentLocationId) ? "instance-updated" : "instance-location-updated";
        StructuredBusinessLogger.info(log, "inventory", action, StructuredBusinessLogger.fields(
                "instance_id", salva.getId(),
                "item_id", salva.getMainItem() == null ? null : salva.getMainItem().getId(),
                "previous_location_id", previousLocationId,
                "location_id", currentLocationId,
                "success", true
        ));
        return ItemInstanceMapper.toResponseDTO(salva);
    }

    @Transactional
    public void deleteLogically(UUID id) {
        if (itemMovementRepository.existsByItemInstanceId(id) || itemLoanRepository.existsByItemInstanceId(id)) {
            throw new IllegalArgumentException("Instance with operational history cannot be deleted. Use the outbound operation to remove it from inventory.");
        }
        ItemInstance instance = findById(id);
        delete(id);
        StructuredBusinessLogger.info(log, "inventory", "instance-deactivated", StructuredBusinessLogger.fields(
                "instance_id", id,
                "item_id", instance.getMainItem() == null ? null : instance.getMainItem().getId(),
                "location_id", instance.getCurrentLocation() == null ? null : instance.getCurrentLocation().getId(),
                "success", true
        ));
    }

    @Transactional(readOnly = true)
    public List<RevisionDTO<ItemInstance>> listRevisions(UUID id) {
        return listPreviousVersions(id);
    }

    private MainItem findActiveMainItem(UUID mainItemId) {
        MainItem mainItem = mainItemRepository.findById(mainItemId)
                .orElseThrow(() -> new IllegalArgumentException("Main item not found."));
        if (!mainItem.isActive()) {
            throw new IllegalArgumentException("Main item must be active.");
        }
        return mainItem;
    }

    private StorageLocation findActiveLocation(UUID locationId) {
        if (locationId == null) {
            return null;
        }

        StorageLocation location = storageLocationRepository.findById(locationId)
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

    private void validateIdentification(ItemInstance instance) {
        if (instance.getIdentifier() == null && instance.getAssetTag() == null && instance.getSerialNumber() == null) {
            throw new IllegalArgumentException("Provide identifier, asset number or serial number for the instance.");
        }
    }
}

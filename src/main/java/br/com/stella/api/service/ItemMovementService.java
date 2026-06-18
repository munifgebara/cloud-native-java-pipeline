package br.com.stella.api.service;

import br.com.munif.common.service.SuperService;
import br.com.munif.common.utils.validacoes.BrValidations;
import br.com.stella.api.dto.ItemInputMovementCreateDTO;
import br.com.stella.api.dto.ItemMovementResponseDTO;
import br.com.stella.api.dto.ItemOutputMovementCreateDTO;
import br.com.stella.api.dto.ItemTransferMovementCreateDTO;
import br.com.stella.api.entity.ItemInstance;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.entity.StorageLocation;
import br.com.stella.api.entity.ItemMovement;
import br.com.stella.api.entity.ItemInstanceStatus;
import br.com.stella.api.entity.ItemMovementType;
import br.com.stella.api.mapper.ItemMovementMapper;
import br.com.stella.api.repository.ItemInstanceRepository;
import br.com.stella.api.repository.MainItemRepository;
import br.com.stella.api.repository.StorageLocationRepository;
import br.com.stella.api.repository.ItemMovementRepository;
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
 * <p>Each operation validates the current state of the instance via {@link ItemInstanceRules}
 * before persisting the movement record.</p>
 */
@Service
public class ItemMovementService extends SuperService<ItemMovement, ItemMovementRepository> {

    private final ItemInstanceRepository itemInstanceRepository;
    private final MainItemRepository mainItemRepository;
    private final StorageLocationRepository storageLocationRepository;

    public ItemMovementService(
            ItemMovementRepository repository,
            EntityManager entityManager,
            ItemInstanceRepository itemInstanceRepository,
            MainItemRepository mainItemRepository,
            StorageLocationRepository storageLocationRepository
    ) {
        super(repository, entityManager, ItemMovement.class);
        this.itemInstanceRepository = itemInstanceRepository;
        this.mainItemRepository = mainItemRepository;
        this.storageLocationRepository = storageLocationRepository;
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
    public ItemMovementResponseDTO registerInbound(ItemInputMovementCreateDTO dto) {
        ItemInstance instance = new ItemInstance();
        instance.setMainItem(findActiveMainItem(dto.mainItemId()));
        instance.setCurrentLocation(findActiveLocation(dto.destinationLocationId()));
        instance.setIdentifier(BrValidations.trimToNull(dto.identifier()));
        instance.setAssetTag(BrValidations.trimToNull(dto.assetTag()));
        instance.setSerialNumber(BrValidations.trimToNull(dto.serialNumber()));
        instance.setNotes(BrValidations.trimToNull(dto.notes()));
        instance.setOperationalStatus(ItemInstanceStatus.DISPONIVEL);

        validateIdentification(instance);

        ItemInstance savedInstance = itemInstanceRepository.save(instance);

        ItemMovement movement = new ItemMovement();
        movement.setType(ItemMovementType.ENTRADA);
        movement.setItemInstance(savedInstance);
        movement.setDestinationLocation(savedInstance.getCurrentLocation());
        movement.setNotes(BrValidations.trimToNull(dto.notes()));

        return ItemMovementMapper.toResponseDTO(save(movement));
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
    public ItemMovementResponseDTO registerOutbound(ItemOutputMovementCreateDTO dto) {
        ItemInstance instance = itemInstanceRepository.findById(dto.itemInstanceId())
                .orElseThrow(() -> new IllegalArgumentException("Instance not found."));
        ItemInstanceRules.requireAvailableWithLocation(
                instance,
                "Instance must be active to register an outbound.",
                "Only available instances can register an outbound.",
                "Instance must have a current location to register an outbound."
        );

        StorageLocation originLocation = instance.getCurrentLocation();
        String reason = BrValidations.trimToNull(dto.reason());
        if (reason == null) {
            throw new IllegalArgumentException("Reason is required.");
        }

        instance.setCurrentLocation(null);
        instance.setOperationalStatus(ItemInstanceStatus.EM_MOVIMENTACAO);
        itemInstanceRepository.save(instance);

        ItemMovement movement = new ItemMovement();
        movement.setType(ItemMovementType.SAIDA);
        movement.setItemInstance(instance);
        movement.setOriginLocation(originLocation);
        movement.setReason(reason);
        movement.setNotes(BrValidations.trimToNull(dto.notes()));

        return ItemMovementMapper.toResponseDTO(save(movement));
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
    public ItemMovementResponseDTO registerTransfer(ItemTransferMovementCreateDTO dto) {
        ItemInstance instance = itemInstanceRepository.findById(dto.itemInstanceId())
                .orElseThrow(() -> new IllegalArgumentException("Instance not found."));
        ItemInstanceRules.requireAvailableWithLocation(
                instance,
                "Instance must be active to register a transfer.",
                "Only available instances can be transferred.",
                "Instance must have a current location to register a transfer."
        );

        StorageLocation originLocation = instance.getCurrentLocation();
        StorageLocation destinationLocation = findActiveLocation(dto.destinationLocationId());
        if (originLocation.getId().equals(destinationLocation.getId())) {
            throw new IllegalArgumentException("Destination location must be different from the current location.");
        }

        instance.setCurrentLocation(destinationLocation);
        instance.setOperationalStatus(ItemInstanceStatus.DISPONIVEL);
        itemInstanceRepository.save(instance);

        ItemMovement movement = new ItemMovement();
        movement.setType(ItemMovementType.TRANSFERENCIA);
        movement.setItemInstance(instance);
        movement.setOriginLocation(originLocation);
        movement.setDestinationLocation(destinationLocation);
        movement.setNotes(BrValidations.trimToNull(dto.notes()));

        return ItemMovementMapper.toResponseDTO(save(movement));
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
        StorageLocation location = storageLocationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Destination location not found."));
        if (!location.isActive()) {
            throw new IllegalArgumentException("Destination location must be active.");
        }
        return location;
    }

    private void validateIdentification(ItemInstance instance) {
        if (instance.getIdentifier() == null && instance.getAssetTag() == null && instance.getSerialNumber() == null) {
            throw new IllegalArgumentException("Provide identifier, asset number or serial number for the instance.");
        }
    }
}

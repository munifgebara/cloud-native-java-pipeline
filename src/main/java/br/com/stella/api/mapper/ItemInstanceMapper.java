package br.com.stella.api.mapper;

import br.com.stella.api.dto.ItemInstanceCreateDTO;
import br.com.stella.api.dto.ItemInstanceResponseDTO;
import br.com.stella.api.dto.ItemInstanceSummaryDTO;
import br.com.stella.api.dto.ItemInstanceUpdateDTO;
import br.com.stella.api.entity.Category;
import br.com.stella.api.entity.ItemInstance;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.entity.StorageLocation;
import br.com.stella.api.entity.ItemInstanceStatus;

public final class ItemInstanceMapper {

    private ItemInstanceMapper() {
    }

    public static ItemInstance toEntity(ItemInstanceCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        ItemInstance instance = new ItemInstance();
        instance.setIdentifier(dto.identifier());
        instance.setAssetTag(dto.assetTag());
        instance.setSerialNumber(dto.serialNumber());
        instance.setOperationalStatus(statusOrDefault(dto.operationalStatus()));
        instance.setNotes(dto.notes());
        instance.setRegistrationOrigin(dto.registrationOrigin());
        if (dto.ativa() != null) {
            instance.setActive(dto.ativa());
        }
        return instance;
    }

    public static void updateEntity(ItemInstance entity, ItemInstanceUpdateDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setIdentifier(dto.identifier());
        entity.setAssetTag(dto.assetTag());
        entity.setSerialNumber(dto.serialNumber());
        entity.setOperationalStatus(statusOrDefault(dto.operationalStatus()));
        entity.setNotes(dto.notes());
        entity.setRegistrationOrigin(dto.registrationOrigin());
        if (dto.ativa() != null) {
            entity.setActive(dto.ativa());
        }
    }

    public static ItemInstanceResponseDTO toResponseDTO(ItemInstance entity) {
        if (entity == null) {
            return null;
        }

        MainItem mainItem = entity.getMainItem();
        Category category = mainItem == null ? null : mainItem.getCategory();
        StorageLocation currentLocation = entity.getCurrentLocation();
        return new ItemInstanceResponseDTO(
                entity.getId(),
                mainItem == null ? null : mainItem.getId(),
                mainItem == null ? null : mainItem.getName(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                category == null ? null : category.getIcon(),
                currentLocation == null ? null : currentLocation.getId(),
                currentLocation == null ? null : currentLocation.getName(),
                entity.getIdentifier(),
                entity.getAssetTag(),
                entity.getSerialNumber(),
                entity.getOperationalStatus(),
                entity.getNotes(),
                entity.getRegistrationOrigin(),
                entity.isActive()
        );
    }

    public static ItemInstanceSummaryDTO toResumoDTO(ItemInstance entity) {
        if (entity == null) {
            return null;
        }

        MainItem mainItem = entity.getMainItem();
        Category category = mainItem == null ? null : mainItem.getCategory();
        StorageLocation currentLocation = entity.getCurrentLocation();
        return new ItemInstanceSummaryDTO(
                entity.getId(),
                mainItem == null ? null : mainItem.getId(),
                mainItem == null ? null : mainItem.getName(),
                category == null ? null : category.getName(),
                category == null ? null : category.getIcon(),
                currentLocation == null ? null : currentLocation.getId(),
                currentLocation == null ? null : currentLocation.getName(),
                entity.getIdentifier(),
                entity.getAssetTag(),
                entity.getSerialNumber(),
                entity.getOperationalStatus(),
                entity.isActive()
        );
    }

    private static ItemInstanceStatus statusOrDefault(ItemInstanceStatus status) {
        return status == null ? ItemInstanceStatus.DISPONIVEL : status;
    }
}

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
        instance.setIdentifier(dto.identificador());
        instance.setAssetTag(dto.patrimonio());
        instance.setSerialNumber(dto.numeroSerie());
        instance.setOperationalStatus(statusOrDefault(dto.statusOperacional()));
        instance.setNotes(dto.observacoes());
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

        entity.setIdentifier(dto.identificador());
        entity.setAssetTag(dto.patrimonio());
        entity.setSerialNumber(dto.numeroSerie());
        entity.setOperationalStatus(statusOrDefault(dto.statusOperacional()));
        entity.setNotes(dto.observacoes());
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
        StorageLocation localAtual = entity.getCurrentLocation();
        return new ItemInstanceResponseDTO(
                entity.getId(),
                mainItem == null ? null : mainItem.getId(),
                mainItem == null ? null : mainItem.getName(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                category == null ? null : category.getIcon(),
                localAtual == null ? null : localAtual.getId(),
                localAtual == null ? null : localAtual.getName(),
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
        StorageLocation localAtual = entity.getCurrentLocation();
        return new ItemInstanceSummaryDTO(
                entity.getId(),
                mainItem == null ? null : mainItem.getId(),
                mainItem == null ? null : mainItem.getName(),
                category == null ? null : category.getName(),
                category == null ? null : category.getIcon(),
                localAtual == null ? null : localAtual.getId(),
                localAtual == null ? null : localAtual.getName(),
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

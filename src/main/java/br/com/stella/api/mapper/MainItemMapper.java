package br.com.stella.api.mapper;

import br.com.stella.api.dto.MainItemCreateDTO;
import br.com.stella.api.dto.MainItemResponseDTO;
import br.com.stella.api.dto.MainItemSummaryDTO;
import br.com.stella.api.dto.MainItemUpdateDTO;
import br.com.stella.api.entity.Category;
import br.com.stella.api.entity.MainItem;

public final class MainItemMapper {

    private MainItemMapper() {
    }

    public static MainItem toEntity(MainItemCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        MainItem item = new MainItem();
        item.setName(dto.name());
        item.setDescription(dto.description());
        item.setNotes(dto.notes());
        item.setRegistrationOrigin(dto.registrationOrigin());
        if (dto.active() != null) {
            item.setActive(dto.active());
        }
        return item;
    }

    public static void updateEntity(MainItem entity, MainItemUpdateDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setNotes(dto.notes());
        entity.setRegistrationOrigin(dto.registrationOrigin());
        if (dto.active() != null) {
            entity.setActive(dto.active());
        }
    }

    public static MainItemResponseDTO toResponseDTO(MainItem entity) {
        if (entity == null) {
            return null;
        }

        Category category = entity.getCategory();
        return new MainItemResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getNotes(),
                entity.getRegistrationOrigin(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                category == null ? null : category.getIcon(),
                imageUrl(entity),
                entity.getImageContentType(),
                entity.getImageSizeBytes(),
                entity.isImageGeneratedByAi(),
                entity.getImageProvider(),
                entity.isActive()
        );
    }

    public static MainItemSummaryDTO toResumoDTO(MainItem entity) {
        if (entity == null) {
            return null;
        }

        Category category = entity.getCategory();
        return new MainItemSummaryDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                category == null ? null : category.getIcon(),
                imageUrl(entity),
                entity.isActive()
        );
    }

    private static String imageUrl(MainItem entity) {
        return entity.getImageObjectKey() == null ? null : "/api/public/main-items/%s/main-image".formatted(entity.getId());
    }
}

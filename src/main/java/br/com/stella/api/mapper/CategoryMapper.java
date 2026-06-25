package br.com.stella.api.mapper;

import br.com.stella.api.dto.CategoryCreateDTO;
import br.com.stella.api.dto.CategoryResponseDTO;
import br.com.stella.api.dto.CategorySummaryDTO;
import br.com.stella.api.dto.CategoryUpdateDTO;
import br.com.stella.api.entity.Category;

/**
 * Converts between the {@link Category} entity and its input and output DTOs.
 *
 * <p>Static utility class — must not be instantiated.
 * Centralizes all mapping logic for {@code Category},
 * avoiding duplication in services and controllers.</p>
 */
public final class CategoryMapper {

    private CategoryMapper() {
    }

    /**
     * Creates a new {@link Category} entity from creation data.
     *
     * @param dto category creation data; may be {@code null}
     * @return new populated {@link Category} instance, or {@code null} if {@code dto} is {@code null}
     */
    public static Category toEntity(CategoryCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        Category category = new Category();
        category.setName(dto.name());
        category.setDescription(dto.description());
        category.setIcon(dto.icon());
        if (dto.active() != null) {
            category.setActive(dto.active());
        }
        if (dto.ownerPublic() != null) {
            category.setOwnerPublic(dto.ownerPublic());
        }
        return category;
    }

    /**
     * Applies update data onto an existing {@link Category} entity.
     *
     * <p>Fields are replaced directly — {@code null} values in the DTO clear
     * the corresponding field in the entity.</p>
     *
     * @param entity entity to be updated; ignored if {@code null}
     * @param dto    update data; ignored if {@code null}
     */
    public static void updateEntity(Category entity, CategoryUpdateDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setIcon(dto.icon());
        if (dto.active() != null) {
            entity.setActive(dto.active());
        }
        if (dto.ownerPublic() != null) {
            entity.setOwnerPublic(dto.ownerPublic());
        }
    }

    /**
     * Converts the {@link Category} entity to the full response DTO.
     *
     * @param entity entity to convert; may be {@code null}
     * @return populated {@link CategoryResponseDTO}, or {@code null} if {@code entity} is {@code null}
     */
    public static CategoryResponseDTO toResponseDTO(Category entity) {
        if (entity == null) {
            return null;
        }

        return new CategoryResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getIcon(),
                entity.isActive()
        );
    }

    /**
     * Converts the {@link Category} entity to the summary DTO used in listings.
     *
     * @param entity entity to convert; may be {@code null}
     * @return populated {@link CategorySummaryDTO}, or {@code null} if {@code entity} is {@code null}
     */
    public static CategorySummaryDTO toResumoDTO(Category entity) {
        if (entity == null) {
            return null;
        }

        return new CategorySummaryDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getIcon(),
                entity.isActive()
        );
    }
}

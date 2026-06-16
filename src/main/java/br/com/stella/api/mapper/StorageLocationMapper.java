package br.com.stella.api.mapper;

import br.com.stella.api.dto.StorageLocationCreateDTO;
import br.com.stella.api.dto.StorageLocationResponseDTO;
import br.com.stella.api.dto.StorageLocationSummaryDTO;
import br.com.stella.api.dto.StorageLocationUpdateDTO;
import br.com.stella.api.entity.StorageLocation;

/**
 * Converts between the {@link StorageLocation} entity and its input and output DTOs.
 *
 * <p>Static utility class — must not be instantiated.
 * Centralizes all mapping logic for {@code StorageLocation},
 * including the calculation of the hierarchical path and depth level.</p>
 */
public final class StorageLocationMapper {

    private StorageLocationMapper() {
    }

    /**
     * Creates a new {@link StorageLocation} entity from creation data.
     *
     * <p>The parent location ({@code paiId}) is not resolved here — it must be associated by the service
     * before persisting, as it requires a repository query.</p>
     *
     * @param dto location creation data; may be {@code null}
     * @return new populated {@link StorageLocation} instance, or {@code null} if {@code dto} is {@code null}
     */
    public static StorageLocation toEntity(StorageLocationCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        StorageLocation location = new StorageLocation();
        location.setName(dto.nome());
        location.setDescription(dto.descricao());
        if (dto.ativa() != null) {
            location.setActive(dto.ativa());
        }
        return location;
    }

    /**
     * Applies update data onto an existing {@link StorageLocation} entity.
     *
     * <p>The parent location is not updated here — it must be resolved and associated by the service.</p>
     *
     * @param entity entity to be updated; ignored if {@code null}
     * @param dto    update data; ignored if {@code null}
     */
    public static void updateEntity(StorageLocation entity, StorageLocationUpdateDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setName(dto.nome());
        entity.setDescription(dto.descricao());
        if (dto.ativa() != null) {
            entity.setActive(dto.ativa());
        }
    }

    /**
     * Converts the {@link StorageLocation} entity to the full response DTO.
     *
     * <p>Automatically calculates the hierarchical path and depth level
     * by traversing the parent location chain.</p>
     *
     * @param entity entity to convert; may be {@code null}
     * @return populated {@link StorageLocationResponseDTO}, or {@code null} if {@code entity} is {@code null}
     */
    public static StorageLocationResponseDTO toResponseDTO(StorageLocation entity) {
        if (entity == null) {
            return null;
        }

        return new StorageLocationResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getParent() == null ? null : entity.getParent().getId(),
                entity.getParent() == null ? null : entity.getParent().getName(),
                caminho(entity),
                nivel(entity),
                imagemUrl(entity),
                entity.getImagemContentType(),
                entity.getImagemTamanhoBytes(),
                entity.isActive()
        );
    }

    /**
     * Converts the {@link StorageLocation} entity to the summary DTO used in listings.
     *
     * <p>Receives the pre-calculated path and level as parameters to avoid recalculating
     * the hierarchy in operations that already know them (e.g.: batch listings).</p>
     *
     * @param entity  entity to convert; may be {@code null}
     * @param caminho pre-calculated hierarchical path (e.g.: {@code "Building A > Room 101"})
     * @param nivel   pre-calculated depth level ({@code 0} for root)
     * @return populated {@link StorageLocationSummaryDTO}, or {@code null} if {@code entity} is {@code null}
     */
    public static StorageLocationSummaryDTO toResumoDTO(StorageLocation entity, String caminho, int nivel) {
        if (entity == null) {
            return null;
        }

        return new StorageLocationSummaryDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getParent() == null ? null : entity.getParent().getId(),
                entity.getParent() == null ? null : entity.getParent().getName(),
                caminho,
                nivel,
                imagemUrl(entity),
                entity.isActive()
        );
    }

    /**
     * Builds the relative URL for accessing the location image.
     *
     * @param entity location whose image path will be checked
     * @return relative URL of the image, or {@code null} if the location has no image
     */
    private static String imagemUrl(StorageLocation entity) {
        if (entity.getImagemObjectKey() == null) {
            return null;
        }
        return "/api/public/locais/%s/imagem".formatted(entity.getId());
    }

    /**
     * Calculates the full path of the location by recursively traversing the parent chain.
     *
     * @param entity location whose path will be calculated
     * @return path in the format {@code "Parent > Child > Grandchild"}, or just the name if it is a root
     */
    private static String caminho(StorageLocation entity) {
        if (entity.getParent() == null) {
            return entity.getName();
        }

        return caminho(entity.getParent()) + " > " + entity.getName();
    }

    /**
     * Calculates the depth level of the location in the hierarchy.
     *
     * @param entity location whose level will be calculated
     * @return {@code 0} for root locations, {@code 1} for direct children, and so on
     */
    private static int nivel(StorageLocation entity) {
        int nivel = 0;
        StorageLocation atual = entity.getParent();
        while (atual != null) {
            nivel++;
            atual = atual.getParent();
        }
        return nivel;
    }
}

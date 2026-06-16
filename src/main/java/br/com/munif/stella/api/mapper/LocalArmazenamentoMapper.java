package br.com.munif.stella.api.mapper;

import br.com.munif.stella.api.dto.LocalArmazenamentoCreateDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoResponseDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoResumoDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoUpdateDTO;
import br.com.munif.stella.api.entity.LocalArmazenamento;

/**
 * Converts between the {@link LocalArmazenamento} entity and its input and output DTOs.
 *
 * <p>Static utility class — must not be instantiated.
 * Centralizes all mapping logic for {@code LocalArmazenamento},
 * including the calculation of the hierarchical path and depth level.</p>
 */
public final class LocalArmazenamentoMapper {

    private LocalArmazenamentoMapper() {
    }

    /**
     * Creates a new {@link LocalArmazenamento} entity from creation data.
     *
     * <p>The parent location ({@code paiId}) is not resolved here — it must be associated by the service
     * before persisting, as it requires a repository query.</p>
     *
     * @param dto location creation data; may be {@code null}
     * @return new populated {@link LocalArmazenamento} instance, or {@code null} if {@code dto} is {@code null}
     */
    public static LocalArmazenamento toEntity(LocalArmazenamentoCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        LocalArmazenamento local = new LocalArmazenamento();
        local.setNome(dto.nome());
        local.setDescricao(dto.descricao());
        if (dto.ativa() != null) {
            local.setAtivo(dto.ativa());
        }
        return local;
    }

    /**
     * Applies update data onto an existing {@link LocalArmazenamento} entity.
     *
     * <p>The parent location is not updated here — it must be resolved and associated by the service.</p>
     *
     * @param entity entity to be updated; ignored if {@code null}
     * @param dto    update data; ignored if {@code null}
     */
    public static void updateEntity(LocalArmazenamento entity, LocalArmazenamentoUpdateDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setNome(dto.nome());
        entity.setDescricao(dto.descricao());
        if (dto.ativa() != null) {
            entity.setAtivo(dto.ativa());
        }
    }

    /**
     * Converts the {@link LocalArmazenamento} entity to the full response DTO.
     *
     * <p>Automatically calculates the hierarchical path and depth level
     * by traversing the parent location chain.</p>
     *
     * @param entity entity to convert; may be {@code null}
     * @return populated {@link LocalArmazenamentoResponseDTO}, or {@code null} if {@code entity} is {@code null}
     */
    public static LocalArmazenamentoResponseDTO toResponseDTO(LocalArmazenamento entity) {
        if (entity == null) {
            return null;
        }

        return new LocalArmazenamentoResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao(),
                entity.getPai() == null ? null : entity.getPai().getId(),
                entity.getPai() == null ? null : entity.getPai().getNome(),
                caminho(entity),
                nivel(entity),
                imagemUrl(entity),
                entity.getImagemContentType(),
                entity.getImagemTamanhoBytes(),
                entity.isAtivo()
        );
    }

    /**
     * Converts the {@link LocalArmazenamento} entity to the summary DTO used in listings.
     *
     * <p>Receives the pre-calculated path and level as parameters to avoid recalculating
     * the hierarchy in operations that already know them (e.g.: batch listings).</p>
     *
     * @param entity  entity to convert; may be {@code null}
     * @param caminho pre-calculated hierarchical path (e.g.: {@code "Building A > Room 101"})
     * @param nivel   pre-calculated depth level ({@code 0} for root)
     * @return populated {@link LocalArmazenamentoResumoDTO}, or {@code null} if {@code entity} is {@code null}
     */
    public static LocalArmazenamentoResumoDTO toResumoDTO(LocalArmazenamento entity, String caminho, int nivel) {
        if (entity == null) {
            return null;
        }

        return new LocalArmazenamentoResumoDTO(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao(),
                entity.getPai() == null ? null : entity.getPai().getId(),
                entity.getPai() == null ? null : entity.getPai().getNome(),
                caminho,
                nivel,
                imagemUrl(entity),
                entity.isAtivo()
        );
    }

    /**
     * Builds the relative URL for accessing the location image.
     *
     * @param entity location whose image path will be checked
     * @return relative URL of the image, or {@code null} if the location has no image
     */
    private static String imagemUrl(LocalArmazenamento entity) {
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
    private static String caminho(LocalArmazenamento entity) {
        if (entity.getPai() == null) {
            return entity.getNome();
        }

        return caminho(entity.getPai()) + " > " + entity.getNome();
    }

    /**
     * Calculates the depth level of the location in the hierarchy.
     *
     * @param entity location whose level will be calculated
     * @return {@code 0} for root locations, {@code 1} for direct children, and so on
     */
    private static int nivel(LocalArmazenamento entity) {
        int nivel = 0;
        LocalArmazenamento atual = entity.getPai();
        while (atual != null) {
            nivel++;
            atual = atual.getPai();
        }
        return nivel;
    }
}

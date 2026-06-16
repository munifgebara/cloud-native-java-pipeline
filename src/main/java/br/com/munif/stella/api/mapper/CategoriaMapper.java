package br.com.munif.stella.api.mapper;

import br.com.munif.stella.api.dto.CategoriaCreateDTO;
import br.com.munif.stella.api.dto.CategoriaResponseDTO;
import br.com.munif.stella.api.dto.CategoriaResumoDTO;
import br.com.munif.stella.api.dto.CategoriaUpdateDTO;
import br.com.munif.stella.api.entity.Categoria;

/**
 * Converts between the {@link Categoria} entity and its input and output DTOs.
 *
 * <p>Static utility class — must not be instantiated.
 * Centralizes all mapping logic for {@code Categoria},
 * avoiding duplication in services and controllers.</p>
 */
public final class CategoriaMapper {

    private CategoriaMapper() {
    }

    /**
     * Creates a new {@link Categoria} entity from creation data.
     *
     * @param dto category creation data; may be {@code null}
     * @return new populated {@link Categoria} instance, or {@code null} if {@code dto} is {@code null}
     */
    public static Categoria toEntity(CategoriaCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        Categoria categoria = new Categoria();
        categoria.setNome(dto.nome());
        categoria.setDescricao(dto.descricao());
        categoria.setIcone(dto.icone());
        if (dto.ativa() != null) {
            categoria.setAtivo(dto.ativa());
        }
        return categoria;
    }

    /**
     * Applies update data onto an existing {@link Categoria} entity.
     *
     * <p>Fields are replaced directly — {@code null} values in the DTO clear
     * the corresponding field in the entity.</p>
     *
     * @param entity entity to be updated; ignored if {@code null}
     * @param dto    update data; ignored if {@code null}
     */
    public static void updateEntity(Categoria entity, CategoriaUpdateDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setNome(dto.nome());
        entity.setDescricao(dto.descricao());
        entity.setIcone(dto.icone());
        if (dto.ativa() != null) {
            entity.setAtivo(dto.ativa());
        }
    }

    /**
     * Converts the {@link Categoria} entity to the full response DTO.
     *
     * @param entity entity to convert; may be {@code null}
     * @return populated {@link CategoriaResponseDTO}, or {@code null} if {@code entity} is {@code null}
     */
    public static CategoriaResponseDTO toResponseDTO(Categoria entity) {
        if (entity == null) {
            return null;
        }

        return new CategoriaResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao(),
                entity.getIcone(),
                entity.isAtivo()
        );
    }

    /**
     * Converts the {@link Categoria} entity to the summary DTO used in listings.
     *
     * @param entity entity to convert; may be {@code null}
     * @return populated {@link CategoriaResumoDTO}, or {@code null} if {@code entity} is {@code null}
     */
    public static CategoriaResumoDTO toResumoDTO(Categoria entity) {
        if (entity == null) {
            return null;
        }

        return new CategoriaResumoDTO(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao(),
                entity.getIcone(),
                entity.isAtivo()
        );
    }
}

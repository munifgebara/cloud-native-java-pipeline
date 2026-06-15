package br.com.munif.stella.api.mapper;

import br.com.munif.stella.api.dto.CategoriaCreateDTO;
import br.com.munif.stella.api.dto.CategoriaResponseDTO;
import br.com.munif.stella.api.dto.CategoriaResumoDTO;
import br.com.munif.stella.api.dto.CategoriaUpdateDTO;
import br.com.munif.stella.api.entity.Categoria;

/**
 * Converte entre a entidade {@link Categoria} e seus DTOs de entrada e saída.
 *
 * <p>Classe utilitária estática — não deve ser instanciada.
 * Centraliza toda a lógica de mapeamento de {@code Categoria},
 * evitando duplicação nos serviços e controllers.</p>
 */
public final class CategoriaMapper {

    private CategoriaMapper() {
    }

    /**
     * Cria uma nova entidade {@link Categoria} a partir dos dados de criação.
     *
     * @param dto dados de criação da categoria; pode ser {@code null}
     * @return nova instância de {@link Categoria} populada, ou {@code null} se {@code dto} for {@code null}
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
     * Aplica os dados de atualização sobre a entidade {@link Categoria} existente.
     *
     * <p>Os campos são substituídos diretamente — valores {@code null} no DTO limpam
     * o campo correspondente na entidade.</p>
     *
     * @param entity entidade a ser atualizada; ignorada se {@code null}
     * @param dto    dados de atualização; ignorados se {@code null}
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
     * Converte a entidade {@link Categoria} para o DTO completo de resposta.
     *
     * @param entity entidade a converter; pode ser {@code null}
     * @return {@link CategoriaResponseDTO} populado, ou {@code null} se {@code entity} for {@code null}
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
     * Converte a entidade {@link Categoria} para o DTO resumido usado em listagens.
     *
     * @param entity entidade a converter; pode ser {@code null}
     * @return {@link CategoriaResumoDTO} populado, ou {@code null} se {@code entity} for {@code null}
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

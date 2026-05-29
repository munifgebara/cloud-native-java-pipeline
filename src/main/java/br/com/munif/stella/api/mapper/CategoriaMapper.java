package br.com.munif.stella.api.mapper;

import br.com.munif.stella.api.dto.CategoriaCreateDTO;
import br.com.munif.stella.api.dto.CategoriaResponseDTO;
import br.com.munif.stella.api.dto.CategoriaResumoDTO;
import br.com.munif.stella.api.dto.CategoriaUpdateDTO;
import br.com.munif.stella.api.entity.Categoria;

public final class CategoriaMapper {

    private CategoriaMapper() {
    }

    public static Categoria toEntity(CategoriaCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        Categoria categoria = new Categoria();
        categoria.setNome(dto.nome());
        categoria.setDescricao(dto.descricao());
        if (dto.ativa() != null) {
            categoria.setAtivo(dto.ativa());
        }
        return categoria;
    }

    public static void updateEntity(Categoria entity, CategoriaUpdateDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setNome(dto.nome());
        entity.setDescricao(dto.descricao());
        if (dto.ativa() != null) {
            entity.setAtivo(dto.ativa());
        }
    }

    public static CategoriaResponseDTO toResponseDTO(Categoria entity) {
        if (entity == null) {
            return null;
        }

        return new CategoriaResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao(),
                entity.isAtivo()
        );
    }

    public static CategoriaResumoDTO toResumoDTO(Categoria entity) {
        if (entity == null) {
            return null;
        }

        return new CategoriaResumoDTO(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao(),
                entity.isAtivo()
        );
    }
}

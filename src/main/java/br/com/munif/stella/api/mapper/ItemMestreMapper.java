package br.com.munif.stella.api.mapper;

import br.com.munif.stella.api.dto.ItemMestreCreateDTO;
import br.com.munif.stella.api.dto.ItemMestreResponseDTO;
import br.com.munif.stella.api.dto.ItemMestreResumoDTO;
import br.com.munif.stella.api.dto.ItemMestreUpdateDTO;
import br.com.munif.stella.api.entity.Categoria;
import br.com.munif.stella.api.entity.ItemMestre;

public final class ItemMestreMapper {

    private ItemMestreMapper() {
    }

    public static ItemMestre toEntity(ItemMestreCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        ItemMestre item = new ItemMestre();
        item.setNome(dto.nome());
        item.setDescricao(dto.descricao());
        item.setObservacoes(dto.observacoes());
        if (dto.ativa() != null) {
            item.setAtivo(dto.ativa());
        }
        return item;
    }

    public static void updateEntity(ItemMestre entity, ItemMestreUpdateDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setNome(dto.nome());
        entity.setDescricao(dto.descricao());
        entity.setObservacoes(dto.observacoes());
        if (dto.ativa() != null) {
            entity.setAtivo(dto.ativa());
        }
    }

    public static ItemMestreResponseDTO toResponseDTO(ItemMestre entity) {
        if (entity == null) {
            return null;
        }

        Categoria categoria = entity.getCategoria();
        return new ItemMestreResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao(),
                entity.getObservacoes(),
                categoria == null ? null : categoria.getId(),
                categoria == null ? null : categoria.getNome(),
                categoria == null ? null : categoria.getIcone(),
                imagemUrl(entity),
                entity.getImagemContentType(),
                entity.getImagemTamanhoBytes(),
                entity.isAtivo()
        );
    }

    public static ItemMestreResumoDTO toResumoDTO(ItemMestre entity) {
        if (entity == null) {
            return null;
        }

        Categoria categoria = entity.getCategoria();
        return new ItemMestreResumoDTO(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao(),
                categoria == null ? null : categoria.getId(),
                categoria == null ? null : categoria.getNome(),
                categoria == null ? null : categoria.getIcone(),
                imagemUrl(entity),
                entity.isAtivo()
        );
    }

    private static String imagemUrl(ItemMestre entity) {
        return entity.getImagemObjectKey() == null ? null : "/api/public/itens-mestre/%s/imagem-principal".formatted(entity.getId());
    }
}

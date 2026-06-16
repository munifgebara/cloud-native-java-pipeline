package br.com.stella.api.mapper;

import br.com.stella.api.dto.MainItemCreateDTO;
import br.com.stella.api.dto.MainItemResponseDTO;
import br.com.stella.api.dto.MainItemSummaryDTO;
import br.com.stella.api.dto.MainItemUpdateDTO;
import br.com.stella.api.entity.Category;
import br.com.stella.api.entity.MainItem;

public final class ItemMestreMapper {

    private ItemMestreMapper() {
    }

    public static MainItem toEntity(MainItemCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        MainItem item = new MainItem();
        item.setName(dto.nome());
        item.setDescription(dto.descricao());
        item.setNotes(dto.observacoes());
        item.setOrigemCadastro(dto.origemCadastro());
        if (dto.ativa() != null) {
            item.setActive(dto.ativa());
        }
        return item;
    }

    public static void updateEntity(MainItem entity, MainItemUpdateDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setName(dto.nome());
        entity.setDescription(dto.descricao());
        entity.setNotes(dto.observacoes());
        entity.setOrigemCadastro(dto.origemCadastro());
        if (dto.ativa() != null) {
            entity.setActive(dto.ativa());
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
                entity.getOrigemCadastro(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                category == null ? null : category.getIcon(),
                imagemUrl(entity),
                entity.getImagemContentType(),
                entity.getImagemTamanhoBytes(),
                entity.isImagemGeneratedByAi(),
                entity.getImagemProvider(),
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
                imagemUrl(entity),
                entity.isActive()
        );
    }

    private static String imagemUrl(MainItem entity) {
        return entity.getImagemObjectKey() == null ? null : "/api/public/itens-mestre/%s/imagem-principal".formatted(entity.getId());
    }
}

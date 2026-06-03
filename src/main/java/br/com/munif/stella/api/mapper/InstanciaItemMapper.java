package br.com.munif.stella.api.mapper;

import br.com.munif.stella.api.dto.InstanciaItemCreateDTO;
import br.com.munif.stella.api.dto.InstanciaItemResponseDTO;
import br.com.munif.stella.api.dto.InstanciaItemResumoDTO;
import br.com.munif.stella.api.dto.InstanciaItemUpdateDTO;
import br.com.munif.stella.api.entity.Categoria;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.ItemMestre;

public final class InstanciaItemMapper {

    private InstanciaItemMapper() {
    }

    public static InstanciaItem toEntity(InstanciaItemCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        InstanciaItem instancia = new InstanciaItem();
        instancia.setIdentificador(dto.identificador());
        instancia.setPatrimonio(dto.patrimonio());
        instancia.setNumeroSerie(dto.numeroSerie());
        instancia.setObservacoes(dto.observacoes());
        if (dto.ativa() != null) {
            instancia.setAtivo(dto.ativa());
        }
        return instancia;
    }

    public static void updateEntity(InstanciaItem entity, InstanciaItemUpdateDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setIdentificador(dto.identificador());
        entity.setPatrimonio(dto.patrimonio());
        entity.setNumeroSerie(dto.numeroSerie());
        entity.setObservacoes(dto.observacoes());
        if (dto.ativa() != null) {
            entity.setAtivo(dto.ativa());
        }
    }

    public static InstanciaItemResponseDTO toResponseDTO(InstanciaItem entity) {
        if (entity == null) {
            return null;
        }

        ItemMestre itemMestre = entity.getItemMestre();
        Categoria categoria = itemMestre == null ? null : itemMestre.getCategoria();
        return new InstanciaItemResponseDTO(
                entity.getId(),
                itemMestre == null ? null : itemMestre.getId(),
                itemMestre == null ? null : itemMestre.getNome(),
                categoria == null ? null : categoria.getId(),
                categoria == null ? null : categoria.getNome(),
                categoria == null ? null : categoria.getIcone(),
                entity.getIdentificador(),
                entity.getPatrimonio(),
                entity.getNumeroSerie(),
                entity.getObservacoes(),
                entity.isAtivo()
        );
    }

    public static InstanciaItemResumoDTO toResumoDTO(InstanciaItem entity) {
        if (entity == null) {
            return null;
        }

        ItemMestre itemMestre = entity.getItemMestre();
        Categoria categoria = itemMestre == null ? null : itemMestre.getCategoria();
        return new InstanciaItemResumoDTO(
                entity.getId(),
                itemMestre == null ? null : itemMestre.getId(),
                itemMestre == null ? null : itemMestre.getNome(),
                categoria == null ? null : categoria.getNome(),
                categoria == null ? null : categoria.getIcone(),
                entity.getIdentificador(),
                entity.getPatrimonio(),
                entity.getNumeroSerie(),
                entity.isAtivo()
        );
    }
}

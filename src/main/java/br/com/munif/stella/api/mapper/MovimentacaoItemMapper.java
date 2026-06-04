package br.com.munif.stella.api.mapper;

import br.com.munif.stella.api.dto.MovimentacaoItemResponseDTO;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.LocalArmazenamento;
import br.com.munif.stella.api.entity.MovimentacaoItem;

public final class MovimentacaoItemMapper {

    private MovimentacaoItemMapper() {
    }

    public static MovimentacaoItemResponseDTO toResponseDTO(MovimentacaoItem entity) {
        if (entity == null) {
            return null;
        }

        InstanciaItem instancia = entity.getInstanciaItem();
        LocalArmazenamento local = entity.getLocalDestino();
        return new MovimentacaoItemResponseDTO(
                entity.getId(),
                entity.getTipo(),
                entity.getDataMovimentacao(),
                instancia == null ? null : instancia.getId(),
                identificacao(instancia),
                local == null ? null : local.getId(),
                local == null ? null : local.getNome(),
                entity.getObservacao()
        );
    }

    private static String identificacao(InstanciaItem instancia) {
        if (instancia == null) {
            return null;
        }
        if (instancia.getIdentificador() != null) {
            return instancia.getIdentificador();
        }
        if (instancia.getPatrimonio() != null) {
            return instancia.getPatrimonio();
        }
        return instancia.getNumeroSerie();
    }
}

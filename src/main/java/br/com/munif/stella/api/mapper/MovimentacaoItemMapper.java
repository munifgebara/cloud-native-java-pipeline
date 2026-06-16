package br.com.munif.stella.api.mapper;

import br.com.munif.stella.api.dto.MovimentacaoItemResponseDTO;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.LocalArmazenamento;
import br.com.munif.stella.api.entity.MovimentacaoItem;

/**
 * Converts between the {@link MovimentacaoItem} entity and its output DTOs.
 *
 * <p>Static utility class — must not be instantiated.
 * As movements do not have their own creation DTO (they are created internally by services),
 * this mapper exposes only the conversion to response.</p>
 */
public final class MovimentacaoItemMapper {

    private MovimentacaoItemMapper() {
    }

    /**
     * Converts the {@link MovimentacaoItem} entity to the full response DTO.
     *
     * <p>Includes denormalized data from the instance, origin location, and destination location
     * to avoid additional requests on the client.</p>
     *
     * @param entity entity to convert; may be {@code null}
     * @return populated {@link MovimentacaoItemResponseDTO}, or {@code null} if {@code entity} is {@code null}
     */
    public static MovimentacaoItemResponseDTO toResponseDTO(MovimentacaoItem entity) {
        if (entity == null) {
            return null;
        }

        InstanciaItem instancia = entity.getInstanciaItem();
        LocalArmazenamento localOrigem = entity.getLocalOrigem();
        LocalArmazenamento localDestino = entity.getLocalDestino();
        return new MovimentacaoItemResponseDTO(
                entity.getId(),
                entity.getTipo(),
                entity.getDataMovimentacao(),
                instancia == null ? null : instancia.getId(),
                identificacao(instancia),
                localOrigem == null ? null : localOrigem.getId(),
                localOrigem == null ? null : localOrigem.getNome(),
                localDestino == null ? null : localDestino.getId(),
                localDestino == null ? null : localDestino.getNome(),
                entity.getMotivo(),
                entity.getObservacao()
        );
    }

    /**
     * Returns the readable identification of the instance, prioritizing:
     * internal identifier, asset number, and lastly serial number.
     *
     * @param instancia instance whose identifier will be resolved; may be {@code null}
     * @return first non-null identification field, or {@code null} if the instance is {@code null}
     */
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

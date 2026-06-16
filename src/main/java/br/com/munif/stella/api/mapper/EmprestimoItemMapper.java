package br.com.munif.stella.api.mapper;

import br.com.munif.stella.api.dto.EmprestimoItemResponseDTO;
import br.com.munif.stella.api.entity.EmprestimoItem;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.Pessoa;

/**
 * Converts between the {@link EmprestimoItem} entity and its output DTOs.
 *
 * <p>Static utility class — must not be instantiated.
 * As loans are managed by specific flows (creation, return),
 * this mapper exposes only the conversion to response.</p>
 */
public final class EmprestimoItemMapper {

    private EmprestimoItemMapper() {
    }

    /**
     * Converts the {@link EmprestimoItem} entity to the full response DTO.
     *
     * <p>Includes denormalized data from the instance and the person to avoid
     * additional requests on the client.</p>
     *
     * @param entity entity to convert; may be {@code null}
     * @return populated {@link EmprestimoItemResponseDTO}, or {@code null} if {@code entity} is {@code null}
     */
    public static EmprestimoItemResponseDTO toResponseDTO(EmprestimoItem entity) {
        if (entity == null) {
            return null;
        }

        InstanciaItem instancia = entity.getInstanciaItem();
        Pessoa pessoa = entity.getPessoa();
        return new EmprestimoItemResponseDTO(
                entity.getId(),
                instancia == null ? null : instancia.getId(),
                identificacao(instancia),
                pessoa == null ? null : pessoa.getId(),
                pessoa == null ? null : pessoa.getNome(),
                entity.getDataEmprestimo(),
                entity.getPrevisaoDevolucao(),
                entity.getDataDevolucao(),
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

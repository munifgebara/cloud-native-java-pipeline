package br.com.stella.api.mapper;

import br.com.stella.api.dto.ItemLoanResponseDTO;
import br.com.stella.api.entity.ItemLoan;
import br.com.stella.api.entity.ItemInstance;
import br.com.stella.api.entity.Person;

/**
 * Converts between the {@link ItemLoan} entity and its output DTOs.
 *
 * <p>Static utility class — must not be instantiated.
 * As loans are managed by specific flows (creation, return),
 * this mapper exposes only the conversion to response.</p>
 */
public final class ItemLoanMapper {

    private ItemLoanMapper() {
    }

    /**
     * Converts the {@link ItemLoan} entity to the full response DTO.
     *
     * <p>Includes denormalized data from the instance and the person to avoid
     * additional requests on the client.</p>
     *
     * @param entity entity to convert; may be {@code null}
     * @return populated {@link ItemLoanResponseDTO}, or {@code null} if {@code entity} is {@code null}
     */
    public static ItemLoanResponseDTO toResponseDTO(ItemLoan entity) {
        if (entity == null) {
            return null;
        }

        ItemInstance instance = entity.getItemInstance();
        Person pessoa = entity.getPerson();
        return new ItemLoanResponseDTO(
                entity.getId(),
                instance == null ? null : instance.getId(),
                identificacao(instance),
                pessoa == null ? null : pessoa.getId(),
                pessoa == null ? null : pessoa.getName(),
                entity.getDataEmprestimo(),
                entity.getExpectedReturnDate(),
                entity.getReturnDate(),
                entity.getNotes()
        );
    }

    /**
     * Returns the readable identification of the instance, prioritizing:
     * internal identifier, asset number, and lastly serial number.
     *
     * @param instance instance whose identifier will be resolved; may be {@code null}
     * @return first non-null identification field, or {@code null} if the instance is {@code null}
     */
    private static String identificacao(ItemInstance instance) {
        if (instance == null) {
            return null;
        }
        if (instance.getIdentifier() != null) {
            return instance.getIdentifier();
        }
        if (instance.getAssetTag() != null) {
            return instance.getAssetTag();
        }
        return instance.getSerialNumber();
    }
}

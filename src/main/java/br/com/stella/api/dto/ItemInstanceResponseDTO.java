package br.com.stella.api.dto;

import br.com.stella.api.entity.ItemInstanceStatus;

import java.util.UUID;

/**
 * Full response DTO of an item instance.
 *
 * <p>Returned in create, update, and individual query operations.
 * Includes all fields of the instance, including those related to the category and registration origin.</p>
 *
 * @param id                unique identifier of the instance
 * @param itemMestreId      identifier of the main item to which this instance belongs
 * @param itemMestreNome    main item name (denormalized)
 * @param categoriaId       category identifier of the item; may be {@code null}
 * @param categoriaNome     category name; may be {@code null}
 * @param categoriaIcone    category icon key; may be {@code null}
 * @param localAtualId      identifier of the current storage location; may be {@code null}
 * @param localAtualNome    current location name (denormalized); may be {@code null}
 * @param identificador     internal identification code of the instance; may be {@code null}
 * @param patrimonio        asset number; may be {@code null}
 * @param numeroSerie       manufacturer's serial number; may be {@code null}
 * @param statusOperacional current operational status of the instance
 * @param observacoes       internal notes about this instance; may be {@code null}
 * @param registrationOrigin    registration origin (e.g.: {@code "MANUAL"}, {@code "FOTO"}); may be {@code null}
 * @param ativa             indicates whether the instance is active in the system
 */
public record ItemInstanceResponseDTO(
        UUID id,
        UUID itemMestreId,
        String itemMestreNome,
        UUID categoriaId,
        String categoriaNome,
        String categoriaIcone,
        UUID localAtualId,
        String localAtualNome,
        String identificador,
        String patrimonio,
        String numeroSerie,
        ItemInstanceStatus statusOperacional,
        String observacoes,
        String registrationOrigin,
        boolean ativa
) {}

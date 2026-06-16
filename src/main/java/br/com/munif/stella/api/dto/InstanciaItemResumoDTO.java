package br.com.munif.stella.api.dto;

import br.com.munif.stella.api.entity.StatusOperacionalInstancia;

import java.util.UUID;

/**
 * Summary DTO of an item instance, used in listings.
 *
 * <p>Contains only the essential fields for quick identification and location
 * of the instance, without including less-used fields such as notes.</p>
 *
 * @param id                unique identifier of the instance
 * @param itemMestreId      identifier of the main item to which this instance belongs
 * @param itemMestreNome    main item name (denormalized to avoid joins on the client)
 * @param categoriaNome     category name of the main item; may be {@code null}
 * @param categoriaIcone    category icon key; may be {@code null}
 * @param localAtualId      identifier of the current storage location; may be {@code null}
 * @param localAtualNome    current location name (denormalized to avoid joins on the client); may be {@code null}
 * @param identificador     internal identification code of the instance; may be {@code null}
 * @param patrimonio        asset number; may be {@code null}
 * @param numeroSerie       manufacturer's serial number; may be {@code null}
 * @param statusOperacional current operational status of the instance
 * @param ativa             indicates whether the instance is active in the system
 */
public record InstanciaItemResumoDTO(
        UUID id,
        UUID itemMestreId,
        String itemMestreNome,
        String categoriaNome,
        String categoriaIcone,
        UUID localAtualId,
        String localAtualNome,
        String identificador,
        String patrimonio,
        String numeroSerie,
        StatusOperacionalInstancia statusOperacional,
        boolean ativa
) {}

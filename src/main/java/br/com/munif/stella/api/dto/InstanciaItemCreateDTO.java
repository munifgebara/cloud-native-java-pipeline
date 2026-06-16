package br.com.munif.stella.api.dto;

import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Creation DTO for an item instance.
 *
 * <p>Contains the data required to register a new physical instance of an
 * {@code ItemMestre} in the system, including identification, initial location
 * and starting operational status.</p>
 *
 * @param itemMestreId      identifier of the main item to which this instance belongs; required
 * @param localAtualId      identifier of the initial storage location of the instance; optional
 * @param identificador     internal identification code of the instance (up to 100 characters); optional
 * @param patrimonio        asset number assigned to the item (up to 100 characters); optional
 * @param numeroSerie       manufacturer's serial number stamped on the equipment (up to 150 characters); optional
 * @param statusOperacional initial operational status; when {@code null}, defaults to {@code DISPONIVEL}
 * @param observacoes       internal notes about this instance (up to 1000 characters); optional
 * @param origemCadastro    registration origin (e.g.: {@code "MANUAL"}, {@code "FOTO"}); up to 50 characters; optional
 * @param ativa             indicates whether the instance should be created as active; when {@code null}, defaults to {@code true}
 */
public record InstanciaItemCreateDTO(
        @NotNull(message = "Main item is required.")
        UUID itemMestreId,

        UUID localAtualId,

        @Size(max = 100, message = "Identifier must not exceed 100 characters.")
        String identificador,

        @Size(max = 100, message = "Asset number must not exceed 100 characters.")
        String patrimonio,

        @Size(max = 150, message = "Serial number must not exceed 150 characters.")
        String numeroSerie,

        StatusOperacionalInstancia statusOperacional,

        @Size(max = 1000, message = "Notes must not exceed 1000 characters.")
        String observacoes,

        @Size(max = 50, message = "Registration origin must not exceed 50 characters.")
        String origemCadastro,

        Boolean ativa
) {}

package br.com.stella.api.dto;

import br.com.stella.api.entity.ItemMovementType;

import java.time.Instant;
import java.util.UUID;

/**
 * Full response DTO of an item movement.
 *
 * <p>Returned in movement registration operations and in the history query
 * of an instance. Includes denormalized origin and destination data to
 * avoid additional requests on the client.</p>
 *
 * @param id                     unique identifier of the movement
 * @param tipo                   movement type ({@code ENTRADA}, {@code SAIDA} or {@code TRANSFERENCIA})
 * @param movementDate       date and time when the movement occurred (UTC)
 * @param instanciaItemId        identifier of the moved item instance
 * @param instanciaIdentificacao readable identification of the instance (identifier, asset number or serial number)
 * @param localOrigemId          identifier of the origin location; {@code null} in inbound movements
 * @param localOrigemNome        origin location name (denormalized); {@code null} in inbound movements
 * @param localDestinoId         identifier of the destination location; {@code null} in outbound movements
 * @param localDestinoNome       destination location name (denormalized); {@code null} in outbound movements
 * @param motivo                 summarized reason for the movement; may be {@code null}
 * @param observacao             complementary notes about the movement; may be {@code null}
 */
public record ItemMovementResponseDTO(
        UUID id,
        ItemMovementType tipo,
        Instant movementDate,
        UUID instanciaItemId,
        String instanciaIdentificacao,
        UUID localOrigemId,
        String localOrigemNome,
        UUID localDestinoId,
        String localDestinoNome,
        String motivo,
        String observacao
) {}

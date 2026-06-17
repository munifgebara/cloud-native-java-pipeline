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
 * @param itemInstanceId        identifier of the moved item instance
 * @param instanceIdentification readable identification of the instance (identifier, asset number or serial number)
 * @param originLocationId          identifier of the origin location; {@code null} in inbound movements
 * @param originLocationName        origin location name (denormalized); {@code null} in inbound movements
 * @param destinationLocationId         identifier of the destination location; {@code null} in outbound movements
 * @param destinationLocationName       destination location name (denormalized); {@code null} in outbound movements
 * @param motivo                 summarized reason for the movement; may be {@code null}
 * @param notes             complementary notes about the movement; may be {@code null}
 */
public record ItemMovementResponseDTO(
        UUID id,
        ItemMovementType tipo,
        Instant movementDate,
        UUID itemInstanceId,
        String instanceIdentification,
        UUID originLocationId,
        String originLocationName,
        UUID destinationLocationId,
        String destinationLocationName,
        String motivo,
        String notes
) {}

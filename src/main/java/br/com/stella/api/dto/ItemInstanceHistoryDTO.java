package br.com.stella.api.dto;

import java.util.List;

/**
 * DTO that aggregates the full data of an item instance and its movement history.
 *
 * <p>Used in the history endpoint to return in a single response both the current
 * data of the instance and the chronological list of all movements already performed.</p>
 *
 * @param instance     full data of the item instance
 * @param movements list of movements of the instance in ascending chronological order;
 *                      may be empty when no movement has been recorded
 */
public record ItemInstanceHistoryDTO(
        ItemInstanceResponseDTO instance,
        List<ItemMovementResponseDTO> movements
) {}

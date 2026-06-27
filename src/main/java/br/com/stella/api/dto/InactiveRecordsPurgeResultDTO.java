package br.com.stella.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Number of inactive operational records physically removed by an administrative purge.
 */
public record InactiveRecordsPurgeResultDTO(
        int itemLoans,
        int itemMovements,
        int mainItemEmbeddings,
        int itemInstances,
        int mainItems,
        int storageLocations,
        int categories,
        int people
) {

    @JsonProperty("total")
    public int total() {
        return itemLoans + itemMovements + mainItemEmbeddings + itemInstances + mainItems
                + storageLocations + categories + people;
    }
}

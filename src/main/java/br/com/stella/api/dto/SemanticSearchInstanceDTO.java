package br.com.stella.api.dto;

import br.com.stella.api.entity.ItemInstanceStatus;

import java.util.UUID;

public record SemanticSearchInstanceDTO(
        UUID id,
        String identifier,
        String assetTag,
        String serialNumber,
        ItemInstanceStatus operationalStatus,
        UUID currentLocationId,
        String currentLocationName
) {}

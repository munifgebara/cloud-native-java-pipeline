package br.com.stella.api.messaging;

import java.time.Instant;
import java.util.UUID;

public record EmbeddingIndexEvent(
        UUID eventId,
        UUID mainItemId,
        EmbeddingIndexEventType eventType,
        String ownerEmail,
        String ownerIssuer,
        Instant createdAt
) {
}

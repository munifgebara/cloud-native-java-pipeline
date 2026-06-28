package br.com.stella.api.service;

import br.com.munif.common.owner.OwnerContext;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.messaging.EmbeddingIndexEvent;
import br.com.stella.api.messaging.EmbeddingIndexEventType;
import br.com.stella.api.repository.MainItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingIndexConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MainItemRepository mainItemRepository;

    @Mock
    private MainItemVectorSearchService vectorSearchService;

    @Mock
    private EmbeddingMessagingMetrics metrics;

    @AfterEach
    void clearOwner() {
        OwnerContext.clear();
    }

    @Test
    void shouldUseExplicitOwnerAndProcessRepeatedUpsertIdempotently() throws Exception {
        EmbeddingIndexEvent event = event(EmbeddingIndexEventType.UPSERT);
        MainItem item = new MainItem();
        item.setId(event.mainItemId());
        when(objectMapper.readValue("payload", EmbeddingIndexEvent.class)).thenReturn(event);
        when(mainItemRepository.findByIdAndOwnerEmailAndOwnerIssuer(
                event.mainItemId(), event.ownerEmail(), event.ownerIssuer())).thenReturn(Optional.of(item));

        consumer().consume("payload");
        consumer().consume("payload");

        verify(vectorSearchService, org.mockito.Mockito.times(2)).synchronize(item);
        verify(metrics, org.mockito.Mockito.times(2)).recordConsumed();
        assertThat(OwnerContext.current()).isEmpty();
    }

    @Test
    void shouldRemoveEmbeddingWithoutLoadingItem() throws Exception {
        EmbeddingIndexEvent event = event(EmbeddingIndexEventType.REMOVE);
        when(objectMapper.readValue("payload", EmbeddingIndexEvent.class)).thenReturn(event);

        consumer().consume("payload");

        verify(vectorSearchService).remove(event.mainItemId());
        verifyNoInteractions(mainItemRepository);
    }

    @Test
    void shouldClearOwnerAndPropagateFailureForRetry() throws Exception {
        EmbeddingIndexEvent event = event(EmbeddingIndexEventType.UPSERT);
        when(objectMapper.readValue("payload", EmbeddingIndexEvent.class)).thenReturn(event);
        when(mainItemRepository.findByIdAndOwnerEmailAndOwnerIssuer(
                event.mainItemId(), event.ownerEmail(), event.ownerIssuer()))
                .thenThrow(new IllegalStateException("temporary"));

        assertThatThrownBy(() -> consumer().consume("payload"))
                .isInstanceOf(IllegalStateException.class);

        verify(metrics).recordRetry();
        assertThat(OwnerContext.current()).isEmpty();
    }

    private EmbeddingIndexConsumer consumer() {
        return new EmbeddingIndexConsumer(objectMapper, mainItemRepository, vectorSearchService, metrics);
    }

    private EmbeddingIndexEvent event(EmbeddingIndexEventType type) {
        return new EmbeddingIndexEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                type,
                "owner@example.test",
                "https://issuer.test/realms/stella",
                Instant.now()
        );
    }
}

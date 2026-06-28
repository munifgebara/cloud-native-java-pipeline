package br.com.stella.api.service;

import br.com.stella.api.config.EmbeddingMessagingProperties;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.messaging.EmbeddingIndexEventType;
import br.com.stella.api.repository.MainItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingIndexDispatcherTest {

    @Mock
    private EmbeddingOutboxService outboxService;

    @Mock
    private MainItemVectorSearchService vectorSearchService;

    @Mock
    private MainItemRepository mainItemRepository;

    @Test
    void shouldEnqueueOperationsWhenMessagingIsEnabled() {
        MainItem item = item();
        EmbeddingIndexDispatcher dispatcher = dispatcher(true);

        dispatcher.dispatchUpsert(item, "create");
        dispatcher.dispatchRemove(item);

        verify(outboxService).enqueue(item, EmbeddingIndexEventType.UPSERT);
        verify(outboxService).enqueue(item, EmbeddingIndexEventType.REMOVE);
    }

    @Test
    void shouldPreserveSynchronousFallbackWhenMessagingIsDisabled() {
        MainItem item = item();
        EmbeddingIndexDispatcher dispatcher = dispatcher(false);

        dispatcher.dispatchUpsert(item, "create");
        dispatcher.dispatchRemove(item);

        verify(vectorSearchService).synchronize(item);
        verify(vectorSearchService).remove(item.getId());
    }

    @Test
    void shouldNotFailBusinessOperationWhenFallbackIndexingFails() {
        MainItem item = item();
        doThrow(new IllegalStateException("embedding unavailable"))
                .when(vectorSearchService).synchronize(item);

        assertThatCode(() -> dispatcher(false).dispatchUpsert(item, "create"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldEnqueueEveryActiveItemForReindex() {
        List<MainItem> items = List.of(item(), item());
        when(mainItemRepository.findAllActive(Sort.by("name"))).thenReturn(items);
        when(outboxService.enqueueAll(items)).thenReturn(2);

        assertThat(dispatcher(true).dispatchReindex()).isEqualTo(2);
        verify(outboxService).enqueueAll(items);
    }

    private EmbeddingIndexDispatcher dispatcher(boolean enabled) {
        return new EmbeddingIndexDispatcher(properties(enabled), outboxService, vectorSearchService, mainItemRepository);
    }

    private EmbeddingMessagingProperties properties(boolean enabled) {
        return new EmbeddingMessagingProperties(enabled, null, null, null, null, null, null, 0, 0, 0);
    }

    private MainItem item() {
        MainItem item = new MainItem();
        item.setId(UUID.randomUUID());
        item.setName("Notebook");
        item.setActive(true);
        item.setOwnerEmail("owner@example.test");
        item.setOwnerIssuer("https://issuer.test/realms/stella");
        return item;
    }
}

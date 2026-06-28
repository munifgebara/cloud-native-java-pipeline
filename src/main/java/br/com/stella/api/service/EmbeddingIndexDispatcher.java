package br.com.stella.api.service;

import br.com.stella.api.config.EmbeddingMessagingProperties;
import br.com.stella.api.dto.SemanticSearchItemDTO;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.messaging.EmbeddingIndexEventType;
import br.com.stella.api.observability.StructuredBusinessLogger;
import br.com.stella.api.repository.MainItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
public class EmbeddingIndexDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingIndexDispatcher.class);

    private final EmbeddingMessagingProperties properties;
    private final EmbeddingOutboxService outboxService;
    private final MainItemVectorSearchService vectorSearchService;
    private final MainItemRepository mainItemRepository;

    public EmbeddingIndexDispatcher(
            EmbeddingMessagingProperties properties,
            EmbeddingOutboxService outboxService,
            MainItemVectorSearchService vectorSearchService,
            MainItemRepository mainItemRepository
    ) {
        this.properties = properties;
        this.outboxService = outboxService;
        this.vectorSearchService = vectorSearchService;
        this.mainItemRepository = mainItemRepository;
    }

    public void dispatchUpsert(MainItem item, String action) {
        if (properties.enabled()) {
            outboxService.enqueue(item, EmbeddingIndexEventType.UPSERT);
            return;
        }
        runAfterCommit(action, item, () -> vectorSearchService.synchronize(item));
    }

    public void dispatchRemove(MainItem item) {
        if (properties.enabled()) {
            outboxService.enqueue(item, EmbeddingIndexEventType.REMOVE);
            return;
        }
        runAfterCommit("item-index-remove-after-delete", item,
                () -> vectorSearchService.remove(item.getId()));
    }

    public int dispatchReindex() {
        if (!properties.enabled()) {
            return vectorSearchService.reindexActiveItems();
        }
        var items = mainItemRepository.findAllActive(Sort.by("name"));
        return outboxService.enqueueAll(items);
    }

    public List<SemanticSearchItemDTO> search(String query) {
        return vectorSearchService.search(query);
    }

    private void runAfterCommit(String action, MainItem item, Runnable operation) {
        Runnable guardedOperation = () -> {
            try {
                operation.run();
            } catch (RuntimeException ex) {
                StructuredBusinessLogger.warn(log, "vector-search", action, StructuredBusinessLogger.fields(
                        "item_id", item == null ? null : item.getId(),
                        "item_name", item == null ? null : item.getName(),
                        "success", false,
                        "failure_type", ex.getClass().getSimpleName()
                ));
            }
        };

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            guardedOperation.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                guardedOperation.run();
            }
        });
    }
}

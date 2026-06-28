package br.com.stella.api.service;

import br.com.stella.api.messaging.EmbeddingIndexEvent;
import br.com.stella.api.messaging.EmbeddingIndexEventType;
import br.com.stella.api.repository.MainItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "stella.messaging.enabled", havingValue = "true")
public class EmbeddingIndexConsumer {

    private final ObjectMapper objectMapper;
    private final MainItemRepository mainItemRepository;
    private final MainItemVectorSearchService vectorSearchService;
    private final EmbeddingMessagingMetrics metrics;

    public EmbeddingIndexConsumer(
            ObjectMapper objectMapper,
            MainItemRepository mainItemRepository,
            MainItemVectorSearchService vectorSearchService,
            EmbeddingMessagingMetrics metrics
    ) {
        this.objectMapper = objectMapper;
        this.mainItemRepository = mainItemRepository;
        this.vectorSearchService = vectorSearchService;
        this.metrics = metrics;
    }

    @RabbitListener(queues = "${stella.messaging.queue:stella.embedding.index}")
    public void consume(String payload) {
        try {
            EmbeddingIndexEvent event = objectMapper.readValue(payload, EmbeddingIndexEvent.class);
            process(event);
            metrics.recordConsumed();
        } catch (RuntimeException ex) {
            metrics.recordRetry();
            throw ex;
        } catch (Exception ex) {
            metrics.recordRetry();
            throw new IllegalArgumentException("Invalid embedding index event.", ex);
        }
    }

    private void process(EmbeddingIndexEvent event) {
        if (event.eventType() == EmbeddingIndexEventType.REMOVE) {
            vectorSearchService.remove(event.mainItemId());
            return;
        }
        mainItemRepository.findByIdAndOwnerEmailAndOwnerIssuer(
                        event.mainItemId(), event.ownerEmail(), event.ownerIssuer())
                .ifPresentOrElse(vectorSearchService::synchronize,
                        () -> vectorSearchService.remove(event.mainItemId()));
    }
}

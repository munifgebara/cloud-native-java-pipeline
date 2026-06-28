package br.com.stella.api.service;

import br.com.stella.api.config.EmbeddingMessagingProperties;
import br.com.stella.api.messaging.EmbeddingIndexEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "stella.messaging.enabled", havingValue = "true")
public class EmbeddingOutboxRelay {

    private final EmbeddingOutboxService outboxService;
    private final EmbeddingMessagingProperties properties;
    private final EmbeddingMessagingMetrics metrics;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public EmbeddingOutboxRelay(
            EmbeddingOutboxService outboxService,
            EmbeddingMessagingProperties properties,
            EmbeddingMessagingMetrics metrics,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper
    ) {
        this.outboxService = outboxService;
        this.properties = properties;
        this.metrics = metrics;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${stella.messaging.relay-delay-ms:1000}")
    public void publishPending() {
        outboxService.findPending(properties.relayBatchSize()).forEach(this::publish);
    }

    private void publish(EmbeddingIndexEvent event) {
        try {
            CorrelationData correlationData = new CorrelationData(event.eventId().toString());
            rabbitTemplate.convertAndSend(
                    properties.exchange(),
                    properties.routingKey(),
                    serialize(event),
                    correlationData
            );
            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(properties.confirmTimeoutSeconds(), TimeUnit.SECONDS);
            if (!confirm.ack()) {
                throw new IllegalStateException("RabbitMQ rejected event: " + confirm.reason());
            }
            if (correlationData.getReturned() != null) {
                throw new IllegalStateException("RabbitMQ returned event without routing it.");
            }
            outboxService.markPublished(event.eventId());
            metrics.recordPublished();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            recordFailure(event, new IllegalStateException("Interrupted while publishing embedding event.", ex));
        } catch (RuntimeException ex) {
            recordFailure(event, ex);
        } catch (Exception ex) {
            recordFailure(event, new IllegalStateException("Unable to confirm embedding event publication.", ex));
        }
    }

    private String serialize(EmbeddingIndexEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize embedding event.", ex);
        }
    }

    private void recordFailure(EmbeddingIndexEvent event, RuntimeException failure) {
        outboxService.markPublishFailure(event.eventId(), failure, properties.publishMaxAttempts());
        metrics.recordPublishFailure();
    }
}

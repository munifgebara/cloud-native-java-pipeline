package br.com.stella.api.service;

import br.com.stella.api.config.EmbeddingMessagingProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "stella.messaging.enabled", havingValue = "true")
public class EmbeddingMessagingMetrics {

    private final Counter published;
    private final Counter publishFailures;
    private final Counter consumed;
    private final Counter retries;

    public EmbeddingMessagingMetrics(
            MeterRegistry meterRegistry,
            EmbeddingOutboxService outboxService,
            AmqpAdmin amqpAdmin,
            EmbeddingMessagingProperties properties
    ) {
        published = Counter.builder("stella.embedding.outbox.published.total").register(meterRegistry);
        publishFailures = Counter.builder("stella.embedding.outbox.publish.failures.total").register(meterRegistry);
        consumed = Counter.builder("stella.embedding.queue.consumed.total").register(meterRegistry);
        retries = Counter.builder("stella.embedding.queue.retries.total").register(meterRegistry);

        Gauge.builder("stella.embedding.outbox.events", outboxService,
                        service -> service.countByStatus("PENDING"))
                .tag("status", "pending")
                .register(meterRegistry);
        Gauge.builder("stella.embedding.outbox.events", outboxService,
                        service -> service.countByStatus("PUBLISHED"))
                .tag("status", "published")
                .register(meterRegistry);
        Gauge.builder("stella.embedding.outbox.events", outboxService,
                        service -> service.countByStatus("FAILED"))
                .tag("status", "failed")
                .register(meterRegistry);
        Gauge.builder("stella.embedding.outbox.oldest.pending.seconds", outboxService,
                        EmbeddingOutboxService::oldestPendingAgeSeconds)
                .register(meterRegistry);
        Gauge.builder("stella.embedding.queue.dead.letter.messages", amqpAdmin,
                        admin -> deadLetterMessages(admin, properties.deadLetterQueue()))
                .register(meterRegistry);
    }

    public void recordPublished() {
        published.increment();
    }

    public void recordPublishFailure() {
        publishFailures.increment();
    }

    public void recordConsumed() {
        consumed.increment();
    }

    public void recordRetry() {
        retries.increment();
    }

    private static double deadLetterMessages(AmqpAdmin amqpAdmin, String queue) {
        try {
            var information = amqpAdmin.getQueueInfo(queue);
            return information == null ? 0 : information.getMessageCount();
        } catch (RuntimeException ignored) {
            return Double.NaN;
        }
    }
}

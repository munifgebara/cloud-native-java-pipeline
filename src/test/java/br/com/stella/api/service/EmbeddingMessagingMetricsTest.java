package br.com.stella.api.service;

import br.com.stella.api.config.EmbeddingMessagingProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmbeddingMessagingMetricsTest {

    @Test
    void shouldExposeCountersAndOperationalGauges() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        EmbeddingOutboxService outboxService = mock(EmbeddingOutboxService.class);
        AmqpAdmin amqpAdmin = mock(AmqpAdmin.class);
        when(outboxService.countByStatus("PENDING")).thenReturn(3L);
        when(outboxService.countByStatus("PUBLISHED")).thenReturn(5L);
        when(outboxService.countByStatus("FAILED")).thenReturn(1L);
        when(outboxService.oldestPendingAgeSeconds()).thenReturn(12.0);
        when(amqpAdmin.getQueueInfo("stella.embedding.index.dlq"))
                .thenReturn(new QueueInformation("stella.embedding.index.dlq", 2, 0));

        EmbeddingMessagingMetrics metrics = new EmbeddingMessagingMetrics(
                registry, outboxService, amqpAdmin, properties());
        metrics.recordPublished();
        metrics.recordPublishFailure();
        metrics.recordConsumed();
        metrics.recordRetry();

        assertThat(registry.get("stella.embedding.outbox.published.total").counter().count()).isOne();
        assertThat(registry.get("stella.embedding.outbox.publish.failures.total").counter().count()).isOne();
        assertThat(registry.get("stella.embedding.queue.consumed.total").counter().count()).isOne();
        assertThat(registry.get("stella.embedding.queue.retries.total").counter().count()).isOne();
        assertThat(registry.get("stella.embedding.outbox.events").tag("status", "pending").gauge().value())
                .isEqualTo(3);
        assertThat(registry.get("stella.embedding.outbox.events").tag("status", "published").gauge().value())
                .isEqualTo(5);
        assertThat(registry.get("stella.embedding.outbox.events").tag("status", "failed").gauge().value())
                .isEqualTo(1);
        assertThat(registry.get("stella.embedding.outbox.oldest.pending.seconds").gauge().value())
                .isEqualTo(12);
        assertThat(registry.get("stella.embedding.queue.dead.letter.messages").gauge().value())
                .isEqualTo(2);
    }

    @Test
    void shouldExposeUnknownDlqDepthWhenBrokerIsUnavailable() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AmqpAdmin amqpAdmin = mock(AmqpAdmin.class);
        when(amqpAdmin.getQueueInfo("stella.embedding.index.dlq"))
                .thenThrow(new IllegalStateException("unavailable"));

        new EmbeddingMessagingMetrics(
                registry, mock(EmbeddingOutboxService.class), amqpAdmin, properties());

        assertThat(registry.get("stella.embedding.queue.dead.letter.messages").gauge().value()).isNaN();
    }

    private EmbeddingMessagingProperties properties() {
        return new EmbeddingMessagingProperties(true, null, null, null, null, null, null, 0, 0, 0);
    }
}

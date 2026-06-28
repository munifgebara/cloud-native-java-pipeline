package br.com.stella.api.service;

import br.com.stella.api.config.EmbeddingMessagingProperties;
import br.com.stella.api.messaging.EmbeddingIndexEvent;
import br.com.stella.api.messaging.EmbeddingIndexEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingOutboxRelayTest {

    @Mock
    private EmbeddingOutboxService outboxService;

    @Mock
    private EmbeddingMessagingMetrics metrics;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void shouldMarkEventPublishedOnlyAfterBrokerConfirmation() throws Exception {
        EmbeddingIndexEvent event = event();
        when(outboxService.findPending(50)).thenReturn(List.of(event));
        when(objectMapper.writeValueAsString(event)).thenReturn("json");
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(eq("stella.embedding"), eq("embedding.index"), eq("json"), any(CorrelationData.class));

        relay().publishPending();

        verify(outboxService).markPublished(event.eventId());
        verify(metrics).recordPublished();
    }

    @Test
    void shouldKeepPendingEventAndRecordFailureWhenBrokerRejectsIt() throws Exception {
        EmbeddingIndexEvent event = event();
        when(outboxService.findPending(50)).thenReturn(List.of(event));
        when(objectMapper.writeValueAsString(event)).thenReturn("json");
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(false, "rejected"));
            return null;
        }).when(rabbitTemplate).convertAndSend(eq("stella.embedding"), eq("embedding.index"), eq("json"), any(CorrelationData.class));

        relay().publishPending();

        verify(outboxService).markPublishFailure(eq(event.eventId()), any(RuntimeException.class), eq(10));
        verify(metrics).recordPublishFailure();
    }

    private EmbeddingOutboxRelay relay() {
        EmbeddingMessagingProperties properties = new EmbeddingMessagingProperties(
                true, null, null, null, null, null, null, 0, 0, 0
        );
        return new EmbeddingOutboxRelay(outboxService, properties, metrics, rabbitTemplate, objectMapper);
    }

    private EmbeddingIndexEvent event() {
        return new EmbeddingIndexEvent(
                UUID.randomUUID(), UUID.randomUUID(), EmbeddingIndexEventType.UPSERT,
                "owner@example.test", "https://issuer.test/realms/stella", Instant.now()
        );
    }
}

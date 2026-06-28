package br.com.stella.api.service;

import br.com.stella.api.entity.MainItem;
import br.com.stella.api.messaging.EmbeddingIndexEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class EmbeddingOutboxServiceIntegrationTest {

    @Autowired
    private EmbeddingOutboxService outboxService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void cleanOutbox() {
        jdbcTemplate.update("delete from public.main_item_embedding_outbox");
    }

    @Test
    void shouldPersistOwnerAndLifecycleOfTransactionalEvent() {
        MainItem item = item();

        UUID eventId = transactionTemplate.execute(status ->
                outboxService.enqueue(item, EmbeddingIndexEventType.UPSERT));

        var events = outboxService.findPending(10);
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.eventId()).isEqualTo(eventId);
            assertThat(event.mainItemId()).isEqualTo(item.getId());
            assertThat(event.ownerEmail()).isEqualTo(item.getOwnerEmail());
            assertThat(event.ownerIssuer()).isEqualTo(item.getOwnerIssuer());
        });

        outboxService.markPublished(eventId);

        assertThat(outboxService.countByStatus("PUBLISHED")).isOne();
        assertThat(outboxService.findPending(10)).isEmpty();
    }

    @Test
    void shouldRollbackOutboxEventWithBusinessTransaction() {
        MainItem item = item();

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            outboxService.enqueue(item, EmbeddingIndexEventType.UPSERT);
            throw new IllegalStateException("rollback");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(outboxService.countByStatus("PENDING")).isZero();
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

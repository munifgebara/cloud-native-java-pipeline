package br.com.stella.api.service;

import br.com.stella.api.entity.MainItem;
import br.com.stella.api.messaging.EmbeddingIndexEvent;
import br.com.stella.api.messaging.EmbeddingIndexEventType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EmbeddingOutboxService {

    private static final String INSERT_SQL = """
            insert into public.main_item_embedding_outbox
                (event_id, main_item_id, event_type, owner_email, owner_issuer)
            values (?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public EmbeddingOutboxService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID enqueue(MainItem item, EmbeddingIndexEventType eventType) {
        if (item == null || item.getId() == null) {
            throw new IllegalArgumentException("A persisted main item is required for embedding outbox events.");
        }
        if (item.getOwnerEmail() == null || item.getOwnerIssuer() == null) {
            throw new IllegalStateException("Embedding outbox events require an explicit owner identity.");
        }

        UUID eventId = UUID.randomUUID();
        jdbcTemplate.update(
                INSERT_SQL,
                eventId,
                item.getId(),
                eventType.name(),
                item.getOwnerEmail(),
                item.getOwnerIssuer()
        );
        return eventId;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int enqueueAll(List<MainItem> items) {
        for (MainItem item : items) {
            enqueue(item, EmbeddingIndexEventType.UPSERT);
        }
        return items.size();
    }

    @Transactional(readOnly = true)
    public List<EmbeddingIndexEvent> findPending(int limit) {
        return jdbcTemplate.query("""
                        select event_id, main_item_id, event_type, owner_email, owner_issuer, created_at
                        from public.main_item_embedding_outbox
                        where status = 'PENDING'
                        order by created_at, event_id
                        limit ?
                        """,
                (rs, rowNum) -> new EmbeddingIndexEvent(
                        rs.getObject("event_id", UUID.class),
                        rs.getObject("main_item_id", UUID.class),
                        EmbeddingIndexEventType.valueOf(rs.getString("event_type")),
                        rs.getString("owner_email"),
                        rs.getString("owner_issuer"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                limit
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(UUID eventId) {
        jdbcTemplate.update("""
                update public.main_item_embedding_outbox
                set status = 'PUBLISHED', attempts = attempts + 1, published_at = current_timestamp, last_error = null
                where event_id = ? and status = 'PENDING'
                """, eventId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublishFailure(UUID eventId, RuntimeException failure, int maxAttempts) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            message = failure.getClass().getSimpleName();
        }
        if (message.length() > 1000) {
            message = message.substring(0, 1000);
        }
        jdbcTemplate.update("""
                update public.main_item_embedding_outbox
                set attempts = attempts + 1,
                    status = case when attempts + 1 >= ? then 'FAILED' else 'PENDING' end,
                    last_error = ?
                where event_id = ? and status = 'PENDING'
                """, maxAttempts, message, eventId);
    }

    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from public.main_item_embedding_outbox where status = ?",
                Long.class,
                status
        );
        return count == null ? 0 : count;
    }

    @Transactional(readOnly = true)
    public double oldestPendingAgeSeconds() {
        Timestamp oldest = jdbcTemplate.queryForObject(
                "select min(created_at) from public.main_item_embedding_outbox where status = 'PENDING'",
                Timestamp.class
        );
        return oldest == null ? 0 : Math.max(0, Duration.between(oldest.toInstant(), Instant.now()).toSeconds());
    }
}

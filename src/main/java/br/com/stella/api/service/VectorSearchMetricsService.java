package br.com.stella.api.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class VectorSearchMetricsService {

    private static final String INSERT_SQL = """
            insert into public.consulta_vetorial_metrica (id, query, quantidade_resultados)
            values (?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public VectorSearchMetricsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordQuery(String query, int resultCount) {
        jdbcTemplate.update(
                INSERT_SQL,
                UUID.randomUUID(),
                query.length() > 1000 ? query.substring(0, 1000) : query,
                resultCount
        );
    }

    @Transactional(readOnly = true)
    public long countQueries() {
        Long total = jdbcTemplate.queryForObject("select count(*) from public.consulta_vetorial_metrica", Long.class);
        return total == null ? 0 : total;
    }
}

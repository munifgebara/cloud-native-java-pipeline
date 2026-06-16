package br.com.stella.api.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ConsultaVetorialMetricasService {

    private static final String INSERT_SQL = """
            insert into public.consulta_vetorial_metrica (id, consulta, quantidade_resultados)
            values (?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public ConsultaVetorialMetricasService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarConsulta(String consulta, int quantidadeResultados) {
        jdbcTemplate.update(
                INSERT_SQL,
                UUID.randomUUID(),
                consulta.length() > 1000 ? consulta.substring(0, 1000) : consulta,
                quantidadeResultados
        );
    }

    @Transactional(readOnly = true)
    public long contarConsultas() {
        Long total = jdbcTemplate.queryForObject("select count(*) from public.consulta_vetorial_metrica", Long.class);
        return total == null ? 0 : total;
    }
}

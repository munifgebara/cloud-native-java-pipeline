package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.entity.LocalArmazenamento;

import java.util.List;

/**
 * JPA repository for persistence operations of {@link LocalArmazenamento}.
 *
 * <p>Extends {@code SuperRepository} which already provides the standard CRUD
 * and pagination methods. The methods declared here are generated automatically by
 * Spring Data JPA from the naming convention.</p>
 */
public interface LocalArmazenamentoRepository extends SuperRepository<LocalArmazenamento> {

    /**
     * Returns all active storage locations, ordered by name in ascending order.
     *
     * @return list of active locations; never {@code null}, may be empty
     */
    List<LocalArmazenamento> findByAtivoTrueOrderByNomeAsc();

    /**
     * Returns active storage locations whose name contains the given substring,
     * case-insensitively, ordered by name in ascending order.
     *
     * @param nome name substring to search (partial, case-insensitive)
     * @return list of matching locations; never {@code null}, may be empty
     */
    List<LocalArmazenamento> findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

    /**
     * Counts the total active storage locations in the system.
     * Used for statistics displayed on the control panel.
     *
     * @return number of locations with {@code ativo = true}
     */
    long countByAtivoTrue();
}

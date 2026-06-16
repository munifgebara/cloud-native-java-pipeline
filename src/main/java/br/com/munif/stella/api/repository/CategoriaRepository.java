package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.entity.Categoria;

import java.util.List;

/**
 * JPA repository for persistence operations of {@link Categoria}.
 *
 * <p>Extends {@code SuperRepository} which already provides the standard CRUD
 * and pagination methods. The methods declared here are generated automatically by
 * Spring Data JPA from the naming convention.</p>
 */
public interface CategoriaRepository extends SuperRepository<Categoria> {

    /**
     * Returns all active categories, ordered by name in ascending order.
     *
     * @return list of active categories; never {@code null}, may be empty
     */
    List<Categoria> findByAtivoTrueOrderByNomeAsc();

    /**
     * Returns active categories whose name contains the given substring,
     * case-insensitively, ordered by name in ascending order.
     *
     * @param nome name substring to search (partial, case-insensitive)
     * @return list of matching categories; never {@code null}, may be empty
     */
    List<Categoria> findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(String nome);
}

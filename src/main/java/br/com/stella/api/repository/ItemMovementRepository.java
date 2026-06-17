package br.com.stella.api.repository;

import br.com.munif.common.persistencia.SuperRepository;
import br.com.stella.api.entity.ItemMovement;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for persistence operations of {@link ItemMovement}.
 *
 * <p>Extends {@code SuperRepository} which already provides the standard CRUD
 * and pagination methods. The methods declared here are generated automatically by
 * Spring Date JPA from the naming convention.</p>
 */
public interface ItemMovementRepository extends SuperRepository<ItemMovement> {

    /**
     * Checks whether any movement is registered for the given item instance.
     * Used to prevent deletion of instances that already have a movement history.
     *
     * @param instanciaItemId identifier of the item instance
     * @return {@code true} if at least one movement exists for this instance
     */
    boolean existsByItemInstanceId(UUID itemInstanceId);

    /**
     * Returns the full movement history of an instance, ordered chronologically.
     *
     * <p>Ordering by {@code dataMovimentacao} and then by {@code criadoEm} ensures
     * consistency when multiple movements occur at the same instant.</p>
     *
     * @param instanciaItemId identifier of the item instance
     * @return list of movements in ascending chronological order; never {@code null}, may be empty
     */
    List<ItemMovement> findByItemInstanceIdOrderByDataMovimentacaoAscCriadoEmAsc(UUID itemInstanceId);
}

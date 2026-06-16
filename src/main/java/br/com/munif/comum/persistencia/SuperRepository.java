package br.com.munif.comum.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.UUID;

/**
 * Base repository for all entities in the system.
 *
 * <p>Extends {@link JpaRepository} with complementary operations to the filtered listings
 * implemented in concrete repositories.
 * The type parameter {@code T} must be a subclass of {@link Entidade}.</p>
 *
 * <p>The {@link NoRepositoryBean} annotation tells Spring Data that this interface
 * should not generate a repository bean on its own — only the concrete subinterfaces
 * receive automatic implementations.</p>
 *
 * @param <T> type of the entity managed by this repository
 */
@NoRepositoryBean
public interface SuperRepository<T extends Entidade> extends JpaRepository<T, UUID> {

    /**
     * Returns all records of the entity, both active and inactive.
     *
     * <p>By default, concrete repositories expose only records with
     * {@code ativo = true}. This method bypasses that filter when it is necessary
     * to view the full history, for example in administrative screens.</p>
     *
     * @return list with all records, regardless of the {@code ativo} field
     */
    default List<T> listarTodosIncluindoInativos() {
        return findAll();
    }
}

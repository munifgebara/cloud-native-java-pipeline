package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.entity.EmprestimoItem;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JPA para operações de persistência de {@link EmprestimoItem}.
 *
 * <p>Estende {@code SuperRepository} que já provê os métodos padrão de CRUD
 * e paginação. Os métodos declarados aqui são gerados automaticamente pelo
 * Spring Data JPA a partir da convenção de nomenclatura.</p>
 */
public interface EmprestimoItemRepository extends SuperRepository<EmprestimoItem> {

    /**
     * Verifica se existe algum empréstimo (ativo ou devolvido) para a instância informada.
     * Utilizado para impedir a exclusão de instâncias que já possuem histórico de empréstimo.
     *
     * @param instanciaItemId identificador da instância de item
     * @return {@code true} se existir ao menos um empréstimo para esta instância
     */
    boolean existsByInstanciaItemId(UUID instanciaItemId);

    /**
     * Verifica se existe um empréstimo ativo (ainda não devolvido) para a instância informada.
     * Um empréstimo ativo possui {@code dataDevolucao} nula.
     * Utilizado para validar se a instância pode ser emprestada novamente.
     *
     * @param instanciaItemId identificador da instância de item
     * @return {@code true} se existir um empréstimo em aberto para esta instância
     */
    boolean existsByInstanciaItemIdAndDataDevolucaoIsNull(UUID instanciaItemId);

    /**
     * Busca o empréstimo ativo (ainda não devolvido) de uma instância de item.
     * Retorna no máximo um resultado, pois uma instância só pode ter um empréstimo ativo por vez.
     *
     * @param instanciaItemId identificador da instância de item
     * @return {@link Optional} com o empréstimo ativo encontrado, ou vazio se não houver empréstimo em aberto
     */
    Optional<EmprestimoItem> findByInstanciaItemIdAndDataDevolucaoIsNull(UUID instanciaItemId);
}

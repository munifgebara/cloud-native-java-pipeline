package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.entity.MovimentacaoItem;

import java.util.List;
import java.util.UUID;

/**
 * Repositório JPA para operações de persistência de {@link MovimentacaoItem}.
 *
 * <p>Estende {@code SuperRepository} que já provê os métodos padrão de CRUD
 * e paginação. Os métodos declarados aqui são gerados automaticamente pelo
 * Spring Data JPA a partir da convenção de nomenclatura.</p>
 */
public interface MovimentacaoItemRepository extends SuperRepository<MovimentacaoItem> {

    /**
     * Verifica se existe alguma movimentação registrada para a instância de item informada.
     * Utilizado para impedir a exclusão de instâncias que já possuem histórico de movimentação.
     *
     * @param instanciaItemId identificador da instância de item
     * @return {@code true} se existir ao menos uma movimentação para esta instância
     */
    boolean existsByInstanciaItemId(UUID instanciaItemId);

    /**
     * Retorna o histórico completo de movimentações de uma instância, ordenado cronologicamente.
     *
     * <p>A ordenação por {@code dataMovimentacao} e depois por {@code criadoEm} garante
     * consistência quando várias movimentações ocorrem no mesmo instante.</p>
     *
     * @param instanciaItemId identificador da instância de item
     * @return lista de movimentações em ordem cronológica crescente; nunca {@code null}, pode ser vazia
     */
    List<MovimentacaoItem> findByInstanciaItemIdOrderByDataMovimentacaoAscCriadoEmAsc(UUID instanciaItemId);
}

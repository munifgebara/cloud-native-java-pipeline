package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.entity.LocalArmazenamento;

import java.util.List;

/**
 * Repositório JPA para operações de persistência de {@link LocalArmazenamento}.
 *
 * <p>Estende {@code SuperRepository} que já provê os métodos padrão de CRUD
 * e paginação. Os métodos declarados aqui são gerados automaticamente pelo
 * Spring Data JPA a partir da convenção de nomenclatura.</p>
 */
public interface LocalArmazenamentoRepository extends SuperRepository<LocalArmazenamento> {

    /**
     * Retorna todos os locais de armazenamento ativos, ordenados pelo nome em ordem crescente.
     *
     * @return lista de locais ativos; nunca {@code null}, pode ser vazia
     */
    List<LocalArmazenamento> findByAtivoTrueOrderByNomeAsc();

    /**
     * Retorna os locais de armazenamento ativos cujo nome contenha o trecho informado,
     * sem distinção de maiúsculas/minúsculas, ordenados pelo nome em ordem crescente.
     *
     * @param nome trecho do nome a pesquisar (busca parcial, case-insensitive)
     * @return lista de locais correspondentes; nunca {@code null}, pode ser vazia
     */
    List<LocalArmazenamento> findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

    /**
     * Conta o total de locais de armazenamento ativos no sistema.
     * Utilizado para estatísticas exibidas no painel de controle.
     *
     * @return quantidade de locais com {@code ativo = true}
     */
    long countByAtivoTrue();
}

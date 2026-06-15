package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.entity.Pessoa;

import java.util.List;
import java.util.Optional;

/**
 * Repositório JPA para operações de persistência de {@link Pessoa}.
 *
 * <p>Estende {@code SuperRepository} que já provê os métodos padrão de CRUD
 * e paginação. Os métodos declarados aqui são gerados automaticamente pelo
 * Spring Data JPA a partir da convenção de nomenclatura.</p>
 */
public interface PessoaRepository extends SuperRepository<Pessoa> {

    /**
     * Busca uma pessoa pelo CPF ou CNPJ exato.
     * Utilizado para verificar duplicidade antes de cadastrar e para autenticar
     * buscas por documento.
     *
     * @param cpfCnpj CPF (11 dígitos) ou CNPJ (14 dígitos) sem formatação
     * @return {@link Optional} com a pessoa encontrada, ou vazio se não existir
     */
    Optional<Pessoa> findByCpfCnpj(String cpfCnpj);

    /**
     * Verifica se já existe uma pessoa cadastrada com o CPF/CNPJ informado.
     * Mais eficiente que {@link #findByCpfCnpj} quando apenas a existência é necessária.
     *
     * @param cpfCnpj CPF ou CNPJ a verificar
     * @return {@code true} se existir uma pessoa com este documento
     */
    boolean existsByCpfCnpj(String cpfCnpj);

    /**
     * Busca pessoas ativas cujo nome contenha o trecho informado,
     * sem distinção de maiúsculas/minúsculas.
     *
     * @param nome trecho do nome a pesquisar (busca parcial, case-insensitive)
     * @return lista de pessoas correspondentes; nunca {@code null}, pode ser vazia
     */
    List<Pessoa> findByAtivoTrueAndNomeContainingIgnoreCase(String nome);

    /**
     * Retorna todas as pessoas ativas, ordenadas pelo nome em ordem crescente.
     *
     * @return lista de pessoas ativas; nunca {@code null}, pode ser vazia
     */
    List<Pessoa> findByAtivoTrueOrderByNomeAsc();

    /**
     * Conta o total de pessoas ativas no sistema.
     * Utilizado para estatísticas exibidas no painel de controle.
     *
     * @return quantidade de pessoas com {@code ativo = true}
     */
    long countByAtivoTrue();
}

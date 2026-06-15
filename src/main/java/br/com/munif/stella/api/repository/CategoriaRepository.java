package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.entity.Categoria;

import java.util.List;

/**
 * Repositório JPA para operações de persistência de {@link Categoria}.
 *
 * <p>Estende {@code SuperRepository} que já provê os métodos padrão de CRUD
 * e paginação. Os métodos declarados aqui são gerados automaticamente pelo
 * Spring Data JPA a partir da convenção de nomenclatura.</p>
 */
public interface CategoriaRepository extends SuperRepository<Categoria> {

    /**
     * Retorna todas as categorias ativas, ordenadas pelo nome em ordem crescente.
     *
     * @return lista de categorias ativas; nunca {@code null}, pode ser vazia
     */
    List<Categoria> findByAtivoTrueOrderByNomeAsc();

    /**
     * Retorna as categorias ativas cujo nome contenha o trecho informado,
     * sem distinção de maiúsculas/minúsculas, ordenadas pelo nome em ordem crescente.
     *
     * @param nome trecho do nome a pesquisar (busca parcial, case-insensitive)
     * @return lista de categorias correspondentes; nunca {@code null}, pode ser vazia
     */
    List<Categoria> findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(String nome);
}

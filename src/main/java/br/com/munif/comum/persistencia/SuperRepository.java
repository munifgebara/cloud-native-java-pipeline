package br.com.munif.comum.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.UUID;

/**
 * Repositório base para todas as entidades do sistema.
 *
 * <p>Estende {@link JpaRepository} com operações complementares às listagens
 * filtradas implementadas nos repositórios concretos.
 * O parâmetro de tipo {@code T} deve ser uma subclasse de {@link Entidade}.</p>
 *
 * <p>A anotação {@link NoRepositoryBean} indica ao Spring Data que esta interface
 * não deve gerar um bean de repositório por si só — apenas as subinterfaces
 * concretas ganham implementações automáticas.</p>
 *
 * @param <T> tipo da entidade gerenciada por este repositório
 */
@NoRepositoryBean
public interface SuperRepository<T extends Entidade> extends JpaRepository<T, UUID> {

    /**
     * Retorna todos os registros da entidade, ativos e inativos.
     *
     * <p>Por padrão, os repositórios concretos expõem apenas registros com
     * {@code ativo = true}. Este método contorna esse filtro quando for necessário
     * visualizar o histórico completo, por exemplo em telas administrativas.</p>
     *
     * @return lista com todos os registros, independentemente do campo {@code ativo}
     */
    default List<T> listarTodosIncluindoInativos() {
        return findAll();
    }
}

package br.com.munif.comum.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.UUID;

@NoRepositoryBean
public interface SuperRepository<T extends Entidade> extends JpaRepository<T, UUID> {

    default List<T> listarTodosIncluindoInativos() {
        return findAll();
    }
}

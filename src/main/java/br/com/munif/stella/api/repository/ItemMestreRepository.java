package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.entity.ItemMestre;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ItemMestreRepository extends SuperRepository<ItemMestre> {

    List<ItemMestre> findByAtivoTrueOrderByNomeAsc();

    List<ItemMestre> findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

    long countByAtivoTrue();

    @Query("""
            select item
            from ItemMestre item
            left join item.categoria categoria
            where item.ativo = true
              and (:nome is null or lower(item.nome) like lower(concat('%', :nome, '%')))
              and (:categoriaId is null or categoria.id = :categoriaId)
            order by item.nome asc
            """)
    List<ItemMestre> filtrarAtivos(@Param("nome") String nome, @Param("categoriaId") UUID categoriaId);
}

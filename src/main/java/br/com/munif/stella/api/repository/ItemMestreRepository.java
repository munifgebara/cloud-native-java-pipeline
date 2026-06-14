package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.dto.DashboardCategoriaQuantidadeDTO;
import br.com.munif.stella.api.entity.ItemMestre;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ItemMestreRepository extends SuperRepository<ItemMestre>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<ItemMestre> {

    List<ItemMestre> findByAtivoTrueOrderByNomeAsc();

    List<ItemMestre> findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

    long countByAtivoTrue();

    long countByAtivoTrueAndImagemObjectKeyIsNull();

    @Query("""
            select count(item)
            from ItemMestre item
            where item.ativo = true
              and (item.origemCadastro = 'CADASTRO_IA_FOTO' or item.imagemGeneratedByAi = true)
            """)
    long contarItensCadastradosPorIa();

    @Query("""
            select new br.com.munif.stella.api.dto.DashboardCategoriaQuantidadeDTO(
                categoria.id,
                categoria.nome,
                count(item)
            )
            from ItemMestre item
            join item.categoria categoria
            where item.ativo = true
              and categoria.ativo = true
            group by categoria.id, categoria.nome
            order by count(item) desc, categoria.nome asc
            """)
    List<DashboardCategoriaQuantidadeDTO> buscarCategoriasComMaisItens(Pageable pageable);

    static Specification<ItemMestre> filtrarAtivos(String nome, UUID categoriaId) {
        return (root, query, cb) -> {
            var predicados = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicados.add(cb.isTrue(root.get("ativo")));
            if (nome != null) {
                predicados.add(cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
            }
            if (categoriaId != null) {
                predicados.add(cb.equal(root.join("categoria").get("id"), categoriaId));
            }
            return cb.and(predicados.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    @Query("""
            select item
            from ItemMestre item
            left join fetch item.categoria
            where item.id in :ids
            """)
    List<ItemMestre> buscarComCategoriaPorIds(@Param("ids") List<UUID> ids);
}

package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.dto.DashboardCategoriaQuantidadeDTO;
import br.com.munif.stella.api.entity.ItemMestre;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persistence repository for {@link ItemMestre}.
 *
 * <p>In addition to the methods inherited from {@link SuperRepository}, it exposes queries
 * by name, filters with {@link Specification}, and projections for the dashboard.</p>
 */
public interface ItemMestreRepository extends SuperRepository<ItemMestre>, JpaSpecificationExecutor<ItemMestre> {

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

    /**
     * Builds a {@link Specification} to filter active main items with optional criteria.
     *
     * <p>{@code null} parameters are simply ignored — no predicate is generated for them.
     * This avoids the null parameter type inference issue in PostgreSQL that occurs
     * with JPQL queries using the {@code (:param is null or ...)} construct.</p>
     *
     * @param nome        substring to search in the item name (case-insensitive); {@code null} ignores the filter
     * @param categoriaId UUID of the category; {@code null} ignores the filter
     * @return specification combining the given filters with {@code AND}
     */
    static Specification<ItemMestre> filtrarAtivos(String nome, UUID categoriaId) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.isTrue(root.get("ativo")));
            if (nome != null) {
                predicados.add(cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
            }
            if (categoriaId != null) {
                predicados.add(cb.equal(root.join("categoria").get("id"), categoriaId));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
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

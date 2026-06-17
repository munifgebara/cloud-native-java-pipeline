package br.com.stella.api.repository;

import br.com.munif.common.persistencia.SuperRepository;
import br.com.stella.api.dto.DashboardCategoryQuantityDTO;
import br.com.stella.api.entity.MainItem;
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
 * Persistence repository for {@link MainItem}.
 *
 * <p>In addition to the methods inherited from {@link SuperRepository}, it exposes queries
 * by name, filters with {@link Specification}, and projections for the dashboard.</p>
 */
public interface MainItemRepository extends SuperRepository<MainItem>, JpaSpecificationExecutor<MainItem> {

    List<MainItem> findByActiveTrueOrderByNameAsc();

    List<MainItem> findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String name);

    long countByActiveTrue();

    long countByActiveTrueAndImageObjectKeyIsNull();

    @Query("""
            select count(item)
            from MainItem item
            where item.active = true
              and (item.registrationOrigin = 'CADASTRO_IA_FOTO' or item.imageGeneratedByAi = true)
            """)
    long countItemsRegisteredByAi();

    @Query("""
            select new br.com.stella.api.dto.DashboardCategoryQuantityDTO(
                category.id,
                category.name,
                count(item)
            )
            from MainItem item
            join item.category category
            where item.active = true
              and category.active = true
            group by category.id, category.name
            order by count(item) desc, category.name asc
            """)
    List<DashboardCategoryQuantityDTO> findCategoriesWithMostItems(Pageable pageable);

    /**
     * Builds a {@link Specification} to filter active main items with optional criteria.
     *
     * <p>{@code null} parameters are simply ignored — no predicate is generated for them.
     * This avoids the null parameter type inference issue in PostgreSQL that occurs
     * with JPQL queries using the {@code (:param is null or ...)} construct.</p>
     *
     * @param name        substring to search in the item name (case-insensitive); {@code null} ignores the filter
     * @param categoryId UUID of the category; {@code null} ignores the filter
     * @return specification combining the given filters with {@code AND}
     */
    static Specification<MainItem> filterActive(String name, UUID categoryId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));
            if (name != null) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.join("category").get("id"), categoryId));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    @Query("""
            select item
            from MainItem item
            left join fetch item.category
            where item.id in :ids
            """)
    List<MainItem> findWithCategoryByIds(@Param("ids") List<UUID> ids);
}

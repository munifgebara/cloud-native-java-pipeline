package br.com.stella.api.repository;

import br.com.munif.common.persistencia.SuperRepository;
import br.com.stella.api.dto.DashboardLocalQuantidadeDTO;
import br.com.stella.api.entity.ItemInstance;
import br.com.stella.api.entity.ItemInstanceStatus;
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
 * Persistence repository for {@link ItemInstance}.
 *
 * <p>In addition to the methods inherited from {@link SuperRepository}, it exposes searches
 * by identifier, dynamic filters with {@link Specification}, and projections
 * for the dashboard and batch searches by main item.</p>
 */
public interface ItemInstanceRepository extends SuperRepository<ItemInstance>, JpaSpecificationExecutor<ItemInstance> {

    List<ItemInstance> findByActiveTrueOrderByIdentifierAscAssetTagAscSerialNumberAsc();

    List<ItemInstance> findByActiveTrueAndIdentifierContainingIgnoreCaseOrderByIdentifierAsc(String identifier);

    @Query("""
            select instance
            from ItemInstance instance
            join fetch instance.mainItem mainItem
            left join fetch instance.currentLocation
            where instance.active = true
              and mainItem.active = true
              and mainItem.id in :mainItemIds
            order by mainItem.name asc, instance.identifier asc, instance.assetTag asc, instance.serialNumber asc
            """)
    List<ItemInstance> buscarAtivasPorItemMestreIds(@Param("mainItemIds") List<UUID> mainItemIds);

    long countByActiveTrue();

    long countByActiveTrueAndOperationalStatus(ItemInstanceStatus operationalStatus);

    /**
     * Builds a {@link Specification} to filter active instances with multiple optional criteria.
     *
     * <p>{@code null} parameters are ignored — no predicate is generated for them.
     * This solves the null parameter type inference issue for UUID and enum parameters
     * in PostgreSQL that occurs when using {@code (:param is null or ...)} in JPQL.</p>
     *
     * @param identificacao     text to search in identifier, asset number or serial number (case-insensitive);
     *                          {@code null} ignores the filter
     * @param mainItem        substring to search in the main item name (case-insensitive);
     *                          {@code null} ignores the filter
     * @param categoriaId       UUID of the main item category; {@code null} ignores the filter
     * @param statusOperacional operational status of the instance; {@code null} ignores the filter
     * @return specification combining the given filters with {@code AND}
     */
    static Specification<ItemInstance> filtrarAtivas(
            String identificacao,
            String mainItem,
            UUID categoriaId,
            ItemInstanceStatus statusOperacional
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.isTrue(root.get("active")));

            if (identificacao != null) {
                String padrao = "%" + identificacao.toLowerCase() + "%";
                predicados.add(cb.or(
                        cb.like(cb.lower(root.get("identifier")), padrao),
                        cb.like(cb.lower(root.get("assetTag")), padrao),
                        cb.like(cb.lower(root.get("serialNumber")), padrao)
                ));
            }

            var mainItemJoin = root.join("mainItem");
            if (mainItem != null) {
                predicados.add(cb.like(cb.lower(mainItemJoin.get("name")), "%" + mainItem.toLowerCase() + "%"));
            }
            if (categoriaId != null) {
                predicados.add(cb.equal(mainItemJoin.join("category").get("id"), categoriaId));
            }
            if (statusOperacional != null) {
                predicados.add(cb.equal(root.get("operationalStatus"), statusOperacional));
            }

            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }

    @Query("""
            select new br.com.stella.api.dto.DashboardLocalQuantidadeDTO(
                location.id,
                location.name,
                count(instance)
            )
            from ItemInstance instance
            join instance.currentLocation location
            where instance.active = true
              and location.active = true
            group by location.id, location.name
            order by count(instance) desc, location.name asc
            """)
    List<DashboardLocalQuantidadeDTO> buscarLocaisComMaisItens(Pageable pageable);
}

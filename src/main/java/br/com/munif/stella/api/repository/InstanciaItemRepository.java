package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.dto.DashboardLocalQuantidadeDTO;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
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
 * Persistence repository for {@link InstanciaItem}.
 *
 * <p>In addition to the methods inherited from {@link SuperRepository}, it exposes searches
 * by identifier, dynamic filters with {@link Specification}, and projections
 * for the dashboard and batch searches by main item.</p>
 */
public interface InstanciaItemRepository extends SuperRepository<InstanciaItem>, JpaSpecificationExecutor<InstanciaItem> {

    List<InstanciaItem> findByAtivoTrueOrderByIdentificadorAscPatrimonioAscNumeroSerieAsc();

    List<InstanciaItem> findByAtivoTrueAndIdentificadorContainingIgnoreCaseOrderByIdentificadorAsc(String identificador);

    @Query("""
            select instancia
            from InstanciaItem instancia
            join fetch instancia.itemMestre itemMestre
            left join fetch instancia.localAtual
            where instancia.ativo = true
              and itemMestre.ativo = true
              and itemMestre.id in :itemMestreIds
            order by itemMestre.nome asc, instancia.identificador asc, instancia.patrimonio asc, instancia.numeroSerie asc
            """)
    List<InstanciaItem> buscarAtivasPorItemMestreIds(@Param("itemMestreIds") List<UUID> itemMestreIds);

    long countByAtivoTrue();

    long countByAtivoTrueAndStatusOperacional(StatusOperacionalInstancia statusOperacional);

    /**
     * Builds a {@link Specification} to filter active instances with multiple optional criteria.
     *
     * <p>{@code null} parameters are ignored — no predicate is generated for them.
     * This solves the null parameter type inference issue for UUID and enum parameters
     * in PostgreSQL that occurs when using {@code (:param is null or ...)} in JPQL.</p>
     *
     * @param identificacao     text to search in identifier, asset number or serial number (case-insensitive);
     *                          {@code null} ignores the filter
     * @param itemMestre        substring to search in the main item name (case-insensitive);
     *                          {@code null} ignores the filter
     * @param categoriaId       UUID of the main item category; {@code null} ignores the filter
     * @param statusOperacional operational status of the instance; {@code null} ignores the filter
     * @return specification combining the given filters with {@code AND}
     */
    static Specification<InstanciaItem> filtrarAtivas(
            String identificacao,
            String itemMestre,
            UUID categoriaId,
            StatusOperacionalInstancia statusOperacional
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.isTrue(root.get("ativo")));

            if (identificacao != null) {
                String padrao = "%" + identificacao.toLowerCase() + "%";
                predicados.add(cb.or(
                        cb.like(cb.lower(root.get("identificador")), padrao),
                        cb.like(cb.lower(root.get("patrimonio")), padrao),
                        cb.like(cb.lower(root.get("numeroSerie")), padrao)
                ));
            }

            var itemMestreJoin = root.join("itemMestre");
            if (itemMestre != null) {
                predicados.add(cb.like(cb.lower(itemMestreJoin.get("nome")), "%" + itemMestre.toLowerCase() + "%"));
            }
            if (categoriaId != null) {
                predicados.add(cb.equal(itemMestreJoin.join("categoria").get("id"), categoriaId));
            }
            if (statusOperacional != null) {
                predicados.add(cb.equal(root.get("statusOperacional"), statusOperacional));
            }

            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }

    @Query("""
            select new br.com.munif.stella.api.dto.DashboardLocalQuantidadeDTO(
                local.id,
                local.nome,
                count(instancia)
            )
            from InstanciaItem instancia
            join instancia.localAtual local
            where instancia.ativo = true
              and local.ativo = true
            group by local.id, local.nome
            order by count(instancia) desc, local.nome asc
            """)
    List<DashboardLocalQuantidadeDTO> buscarLocaisComMaisItens(Pageable pageable);
}

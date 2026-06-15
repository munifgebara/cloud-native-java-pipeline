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
 * Repositório de persistência para {@link InstanciaItem}.
 *
 * <p>Além dos métodos herdados de {@link SuperRepository}, expõe buscas
 * por identificador, filtros dinâmicos com {@link Specification} e projeções
 * para o dashboard e buscas em lote por item mestre.</p>
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
     * Constrói uma {@link Specification} para filtrar instâncias ativas com múltiplos critérios opcionais.
     *
     * <p>Parâmetros {@code null} são ignorados — nenhum predicado é gerado para eles.
     * Isso resolve o problema de inferência de tipo para parâmetros nulos com UUID e enums
     * no PostgreSQL, que ocorre quando se usa {@code (:param is null or ...)} em JPQL.</p>
     *
     * @param identificacao  texto a buscar em identificador, patrimônio ou número de série (case-insensitive);
     *                       {@code null} ignora o filtro
     * @param itemMestre     substring a buscar no nome do item mestre (case-insensitive);
     *                       {@code null} ignora o filtro
     * @param categoriaId    UUID da categoria do item mestre; {@code null} ignora o filtro
     * @param statusOperacional status operacional da instância; {@code null} ignora o filtro
     * @return especificação combinando os filtros informados com {@code AND}
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

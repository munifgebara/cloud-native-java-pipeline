package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.dto.DashboardLocalQuantidadeDTO;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

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

    static Specification<InstanciaItem> filtrarAtivas(String identificacao, String itemMestre, UUID categoriaId, StatusOperacionalInstancia statusOperacional) {
        return (root, query, cb) -> {
            var predicados = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicados.add(cb.isTrue(root.get("ativo")));
            if (identificacao != null) {
                String pattern = "%" + identificacao.toLowerCase() + "%";
                predicados.add(cb.or(
                        cb.like(cb.lower(root.get("identificador")), pattern),
                        cb.like(cb.lower(root.get("patrimonio")), pattern),
                        cb.like(cb.lower(root.get("numeroSerie")), pattern)
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
            return cb.and(predicados.toArray(new jakarta.persistence.criteria.Predicate[0]));
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

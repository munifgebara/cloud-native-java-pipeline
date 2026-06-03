package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InstanciaItemRepository extends SuperRepository<InstanciaItem> {

    List<InstanciaItem> findByAtivoTrueOrderByIdentificadorAscPatrimonioAscNumeroSerieAsc();

    List<InstanciaItem> findByAtivoTrueAndIdentificadorContainingIgnoreCaseOrderByIdentificadorAsc(String identificador);

    @Query("""
            select instancia
            from InstanciaItem instancia
            join instancia.itemMestre itemMestre
            left join itemMestre.categoria categoria
            where instancia.ativo = true
              and (
                    :identificacao is null
                    or lower(instancia.identificador) like lower(concat('%', :identificacao, '%'))
                    or lower(instancia.patrimonio) like lower(concat('%', :identificacao, '%'))
                    or lower(instancia.numeroSerie) like lower(concat('%', :identificacao, '%'))
                  )
              and (:itemMestre is null or lower(itemMestre.nome) like lower(concat('%', :itemMestre, '%')))
              and (:categoriaId is null or categoria.id = :categoriaId)
              and (:statusOperacional is null or instancia.statusOperacional = :statusOperacional)
            order by instancia.identificador asc, instancia.patrimonio asc, instancia.numeroSerie asc
            """)
    List<InstanciaItem> filtrarAtivas(
            @Param("identificacao") String identificacao,
            @Param("itemMestre") String itemMestre,
            @Param("categoriaId") UUID categoriaId,
            @Param("statusOperacional") StatusOperacionalInstancia statusOperacional
    );
}

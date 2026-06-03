package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.entity.InstanciaItem;

import java.util.List;

public interface InstanciaItemRepository extends SuperRepository<InstanciaItem> {

    List<InstanciaItem> findByAtivoTrueOrderByIdentificadorAscPatrimonioAscNumeroSerieAsc();

    List<InstanciaItem> findByAtivoTrueAndIdentificadorContainingIgnoreCaseOrderByIdentificadorAsc(String identificador);
}

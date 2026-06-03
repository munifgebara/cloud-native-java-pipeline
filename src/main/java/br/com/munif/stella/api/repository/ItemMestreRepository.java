package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.entity.ItemMestre;

import java.util.List;

public interface ItemMestreRepository extends SuperRepository<ItemMestre> {

    List<ItemMestre> findByAtivoTrueOrderByNomeAsc();

    List<ItemMestre> findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(String nome);
}

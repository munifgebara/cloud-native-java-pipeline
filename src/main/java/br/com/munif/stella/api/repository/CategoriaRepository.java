package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.entity.Categoria;

import java.util.List;

public interface CategoriaRepository extends SuperRepository<Categoria> {

    List<Categoria> findByAtivoTrueOrderByNomeAsc();

    List<Categoria> findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(String nome);
}

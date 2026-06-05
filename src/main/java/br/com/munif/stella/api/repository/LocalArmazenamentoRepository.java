package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.entity.LocalArmazenamento;

import java.util.List;

public interface LocalArmazenamentoRepository extends SuperRepository<LocalArmazenamento> {

    List<LocalArmazenamento> findByAtivoTrueOrderByNomeAsc();

    List<LocalArmazenamento> findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

    long countByAtivoTrue();
}

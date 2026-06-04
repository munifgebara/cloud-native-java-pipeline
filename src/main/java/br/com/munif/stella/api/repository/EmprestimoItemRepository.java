package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.entity.EmprestimoItem;

import java.util.Optional;
import java.util.UUID;

public interface EmprestimoItemRepository extends SuperRepository<EmprestimoItem> {

    boolean existsByInstanciaItemIdAndDataDevolucaoIsNull(UUID instanciaItemId);

    Optional<EmprestimoItem> findByInstanciaItemIdAndDataDevolucaoIsNull(UUID instanciaItemId);
}

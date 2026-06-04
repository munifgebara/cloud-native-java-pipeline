package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.entity.EmprestimoItem;

import java.util.UUID;

public interface EmprestimoItemRepository extends SuperRepository<EmprestimoItem> {

    boolean existsByInstanciaItemIdAndDataDevolucaoIsNull(UUID instanciaItemId);
}

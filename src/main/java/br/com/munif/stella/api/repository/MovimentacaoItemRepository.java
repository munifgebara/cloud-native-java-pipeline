package br.com.munif.stella.api.repository;

import br.com.munif.comum.persistencia.SuperRepository;
import br.com.munif.stella.api.entity.MovimentacaoItem;

import java.util.List;
import java.util.UUID;

public interface MovimentacaoItemRepository extends SuperRepository<MovimentacaoItem> {

    List<MovimentacaoItem> findByInstanciaItemIdOrderByDataMovimentacaoAscCriadoEmAsc(UUID instanciaItemId);
}

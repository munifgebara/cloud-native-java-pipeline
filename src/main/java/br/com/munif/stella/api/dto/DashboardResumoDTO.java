package br.com.munif.stella.api.dto;

import java.util.List;

public record DashboardResumoDTO(
        long quantidadePessoas,
        long quantidadeItensMestre,
        long quantidadeInstancias,
        long quantidadeInstanciasDisponiveis,
        long quantidadeInstanciasEmprestadas,
        long quantidadeLocais,
        List<DashboardLocalQuantidadeDTO> locaisComMaisItens
) {
}

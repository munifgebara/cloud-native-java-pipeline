package br.com.munif.stella.api.dto;

import java.util.List;

public record DashboardResumoDTO(
        long quantidadePessoas,
        long quantidadeItensMestre,
        long quantidadeInstancias,
        long quantidadeInstanciasDisponiveis,
        long quantidadeInstanciasEmprestadas,
        long quantidadeLocais,
        long quantidadeItensSemImagem,
        long quantidadeItensCadastradosPorIa,
        long quantidadeConsultasVetoriais,
        List<DashboardLocalQuantidadeDTO> locaisComMaisItens,
        List<DashboardCategoriaQuantidadeDTO> categoriasComMaisItens
) {
}

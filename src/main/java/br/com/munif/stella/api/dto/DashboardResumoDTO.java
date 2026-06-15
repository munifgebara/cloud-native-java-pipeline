package br.com.munif.stella.api.dto;

import java.util.List;

/**
 * DTO com os dados consolidados exibidos no painel de controle (dashboard).
 *
 * <p>Agrega contadores globais do inventário e rankings de locais e categorias,
 * permitindo uma visão rápida do estado do sistema na tela inicial.</p>
 *
 * @param quantidadePessoas               total de pessoas cadastradas e ativas
 * @param quantidadeItensMestre           total de itens mestres cadastrados e ativos
 * @param quantidadeInstancias            total de instâncias de itens cadastradas e ativas
 * @param quantidadeInstanciasDisponiveis instâncias com status {@code DISPONIVEL}
 * @param quantidadeInstanciasEmprestadas instâncias com status {@code EMPRESTADO} (empréstimos em aberto)
 * @param quantidadeLocais                total de locais de armazenamento ativos
 * @param quantidadeItensSemImagem        itens mestres ativos que ainda não possuem imagem cadastrada
 * @param quantidadeItensCadastradosPorIa itens mestres cujas imagens foram geradas por inteligência artificial
 * @param quantidadeConsultasVetoriais    total de consultas semânticas (busca vetorial) realizadas no sistema
 * @param locaisComMaisItens              ranking dos locais com maior quantidade de instâncias armazenadas
 * @param categoriasComMaisItens          ranking das categorias com maior quantidade de itens mestres
 */
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

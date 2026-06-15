package br.com.munif.stella.api.dto;

import java.util.UUID;

/**
 * DTO resumido de um local de armazenamento, utilizado em listagens.
 *
 * <p>Inclui informações de hierarquia (pai, caminho e nível) para facilitar
 * a exibição em estruturas de árvore e listas hierárquicas na interface.</p>
 *
 * @param id        identificador único do local
 * @param nome      nome do local
 * @param descricao descrição do local; pode ser {@code null}
 * @param paiId     identificador do local pai; {@code null} para locais raiz
 * @param paiNome   nome do local pai (desnormalizado); {@code null} para locais raiz
 * @param caminho   caminho completo do local na hierarquia (ex.: {@code "Prédio A > Sala 101 > Armário 2"})
 * @param nivel     profundidade na hierarquia: {@code 0} para locais raiz, {@code 1} para filhos, etc.
 * @param imagemUrl URL relativa para acesso à imagem do local; {@code null} quando sem imagem
 * @param ativa     indica se o local está ativo no sistema
 */
public record LocalArmazenamentoResumoDTO(
        UUID id,
        String nome,
        String descricao,
        UUID paiId,
        String paiNome,
        String caminho,
        int nivel,
        String imagemUrl,
        boolean ativa
) {}

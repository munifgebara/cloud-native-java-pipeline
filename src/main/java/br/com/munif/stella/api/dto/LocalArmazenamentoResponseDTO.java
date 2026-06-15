package br.com.munif.stella.api.dto;

import java.util.UUID;

/**
 * DTO completo de resposta de um local de armazenamento.
 *
 * <p>Retornado nas operações de criação, atualização e consulta individual.
 * Inclui metadados completos da imagem e informações de hierarquia.</p>
 *
 * @param id                 identificador único do local
 * @param nome               nome do local
 * @param descricao          descrição do local; pode ser {@code null}
 * @param paiId              identificador do local pai; {@code null} para locais raiz
 * @param paiNome            nome do local pai (desnormalizado); {@code null} para locais raiz
 * @param caminho            caminho completo na hierarquia (ex.: {@code "Prédio A > Sala 101"})
 * @param nivel              profundidade na hierarquia: {@code 0} para locais raiz
 * @param imagemUrl          URL relativa para acesso à imagem; {@code null} quando sem imagem
 * @param imagemContentType  tipo MIME da imagem (ex.: {@code "image/jpeg"}); pode ser {@code null}
 * @param imagemTamanhoBytes tamanho da imagem em bytes; pode ser {@code null}
 * @param ativa              indica se o local está ativo no sistema
 */
public record LocalArmazenamentoResponseDTO(
        UUID id,
        String nome,
        String descricao,
        UUID paiId,
        String paiNome,
        String caminho,
        int nivel,
        String imagemUrl,
        String imagemContentType,
        Long imagemTamanhoBytes,
        boolean ativa
) {}

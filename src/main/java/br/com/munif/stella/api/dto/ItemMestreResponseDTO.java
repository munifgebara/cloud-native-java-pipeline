package br.com.munif.stella.api.dto;

import java.util.UUID;

/**
 * DTO completo de resposta de um item mestre.
 *
 * <p>Retornado nas operações de criação, atualização e consulta individual.
 * Inclui todos os campos do item mestre, com metadados completos de imagem
 * para exibição e para verificação de autoria (gerada por IA ou não).</p>
 *
 * @param id                   identificador único do item mestre
 * @param nome                 nome do item mestre
 * @param descricao            descrição detalhada; pode ser {@code null}
 * @param observacoes          observações internas; pode ser {@code null}
 * @param origemCadastro       origem do cadastro (ex.: {@code "MANUAL"}, {@code "IA"}); pode ser {@code null}
 * @param categoriaId          identificador da categoria; pode ser {@code null}
 * @param categoriaNome        nome da categoria (desnormalizado); pode ser {@code null}
 * @param categoriaIcone       chave do ícone da categoria; pode ser {@code null}
 * @param imagemUrl            URL relativa para acesso à imagem; {@code null} quando sem imagem
 * @param imagemContentType    tipo MIME da imagem (ex.: {@code "image/jpeg"}); pode ser {@code null}
 * @param imagemTamanhoBytes   tamanho da imagem em bytes; pode ser {@code null}
 * @param imagemGeneratedByAi  {@code true} se a imagem foi gerada por IA
 * @param imagemProvider       identificador do provedor de IA que gerou a imagem; pode ser {@code null}
 * @param ativa                indica se o item está ativo no sistema
 */
public record ItemMestreResponseDTO(
        UUID id,
        String nome,
        String descricao,
        String observacoes,
        String origemCadastro,
        UUID categoriaId,
        String categoriaNome,
        String categoriaIcone,
        String imagemUrl,
        String imagemContentType,
        Long imagemTamanhoBytes,
        boolean imagemGeneratedByAi,
        String imagemProvider,
        boolean ativa
) {}

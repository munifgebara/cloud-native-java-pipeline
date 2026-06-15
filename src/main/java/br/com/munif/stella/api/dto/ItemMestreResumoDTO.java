package br.com.munif.stella.api.dto;

import java.util.UUID;

/**
 * DTO resumido de um item mestre, utilizado em listagens e seletores.
 *
 * <p>Contém apenas os campos mais relevantes para exibição em grades e listas,
 * incluindo a URL da imagem para visualização em miniatura.</p>
 *
 * @param id             identificador único do item mestre
 * @param nome           nome do item mestre
 * @param descricao      descrição breve do item; pode ser {@code null}
 * @param categoriaId    identificador da categoria associada; pode ser {@code null}
 * @param categoriaNome  nome da categoria (desnormalizado); pode ser {@code null}
 * @param categoriaIcone chave do ícone da categoria; pode ser {@code null}
 * @param imagemUrl      URL relativa para acesso à imagem do item; {@code null} quando sem imagem
 * @param ativa          indica se o item está ativo no sistema
 */
public record ItemMestreResumoDTO(
        UUID id,
        String nome,
        String descricao,
        UUID categoriaId,
        String categoriaNome,
        String categoriaIcone,
        String imagemUrl,
        boolean ativa
) {}

package br.com.munif.stella.api.dto;

import br.com.munif.stella.api.entity.StatusOperacionalInstancia;

import java.util.UUID;

/**
 * DTO completo de resposta de uma instância de item.
 *
 * <p>Retornado nas operações de criação, atualização e consulta individual.
 * Inclui todos os campos da instância, inclusive os relacionados à categoria e à origem do cadastro.</p>
 *
 * @param id                identificador único da instância
 * @param itemMestreId      identificador do item mestre ao qual esta instância pertence
 * @param itemMestreNome    nome do item mestre (desnormalizado)
 * @param categoriaId       identificador da categoria do item; pode ser {@code null}
 * @param categoriaNome     nome da categoria; pode ser {@code null}
 * @param categoriaIcone    chave do ícone da categoria; pode ser {@code null}
 * @param localAtualId      identificador do local de armazenamento atual; pode ser {@code null}
 * @param localAtualNome    nome do local atual (desnormalizado); pode ser {@code null}
 * @param identificador     código interno de identificação da instância; pode ser {@code null}
 * @param patrimonio        número de patrimônio; pode ser {@code null}
 * @param numeroSerie       número de série do fabricante; pode ser {@code null}
 * @param statusOperacional status operacional atual da instância
 * @param observacoes       observações internas sobre esta instância; pode ser {@code null}
 * @param origemCadastro    origem do cadastro (ex.: {@code "MANUAL"}, {@code "FOTO"}); pode ser {@code null}
 * @param ativa             indica se a instância está ativa no sistema
 */
public record InstanciaItemResponseDTO(
        UUID id,
        UUID itemMestreId,
        String itemMestreNome,
        UUID categoriaId,
        String categoriaNome,
        String categoriaIcone,
        UUID localAtualId,
        String localAtualNome,
        String identificador,
        String patrimonio,
        String numeroSerie,
        StatusOperacionalInstancia statusOperacional,
        String observacoes,
        String origemCadastro,
        boolean ativa
) {}

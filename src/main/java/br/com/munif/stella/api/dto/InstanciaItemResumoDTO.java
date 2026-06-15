package br.com.munif.stella.api.dto;

import br.com.munif.stella.api.entity.StatusOperacionalInstancia;

import java.util.UUID;

/**
 * DTO resumido de uma instância de item, utilizado em listagens.
 *
 * <p>Contém apenas os campos essenciais para identificação e localização rápida
 * da instância, sem incluir campos menos utilizados como observações.</p>
 *
 * @param id                identificador único da instância
 * @param itemMestreId      identificador do item mestre ao qual esta instância pertence
 * @param itemMestreNome    nome do item mestre (desnormalizado para evitar joins no cliente)
 * @param categoriaNome     nome da categoria do item mestre; pode ser {@code null}
 * @param categoriaIcone    chave do ícone da categoria; pode ser {@code null}
 * @param localAtualId      identificador do local de armazenamento atual; pode ser {@code null}
 * @param localAtualNome    nome do local atual (desnormalizado para evitar joins no cliente); pode ser {@code null}
 * @param identificador     código interno de identificação da instância; pode ser {@code null}
 * @param patrimonio        número de patrimônio; pode ser {@code null}
 * @param numeroSerie       número de série do fabricante; pode ser {@code null}
 * @param statusOperacional status operacional atual da instância
 * @param ativa             indica se a instância está ativa no sistema
 */
public record InstanciaItemResumoDTO(
        UUID id,
        UUID itemMestreId,
        String itemMestreNome,
        String categoriaNome,
        String categoriaIcone,
        UUID localAtualId,
        String localAtualNome,
        String identificador,
        String patrimonio,
        String numeroSerie,
        StatusOperacionalInstancia statusOperacional,
        boolean ativa
) {}

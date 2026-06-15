package br.com.munif.stella.api.dto;

import br.com.munif.stella.api.entity.TipoMovimentacaoItem;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO completo de resposta de uma movimentação de item.
 *
 * <p>Retornado nas operações de registro de movimentação e na consulta do histórico
 * de uma instância. Inclui os dados desnormalizados de origem e destino para
 * evitar requisições adicionais no cliente.</p>
 *
 * @param id                   identificador único da movimentação
 * @param tipo                 tipo da movimentação ({@code ENTRADA}, {@code SAIDA} ou {@code TRANSFERENCIA})
 * @param dataMovimentacao     data e hora em que a movimentação ocorreu (UTC)
 * @param instanciaItemId      identificador da instância de item movimentada
 * @param instanciaIdentificacao identificação legível da instância (identificador, patrimônio ou número de série)
 * @param localOrigemId        identificador do local de origem; {@code null} em movimentações de entrada
 * @param localOrigemNome      nome do local de origem (desnormalizado); {@code null} em movimentações de entrada
 * @param localDestinoId       identificador do local de destino; {@code null} em movimentações de saída
 * @param localDestinoNome     nome do local de destino (desnormalizado); {@code null} em movimentações de saída
 * @param motivo               motivo resumido da movimentação; pode ser {@code null}
 * @param observacao           observações complementares sobre a movimentação; pode ser {@code null}
 */
public record MovimentacaoItemResponseDTO(
        UUID id,
        TipoMovimentacaoItem tipo,
        Instant dataMovimentacao,
        UUID instanciaItemId,
        String instanciaIdentificacao,
        UUID localOrigemId,
        String localOrigemNome,
        UUID localDestinoId,
        String localDestinoNome,
        String motivo,
        String observacao
) {}

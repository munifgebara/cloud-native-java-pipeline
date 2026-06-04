package br.com.munif.stella.api.dto;

import br.com.munif.stella.api.entity.TipoMovimentacaoItem;

import java.time.Instant;
import java.util.UUID;

public record MovimentacaoItemResponseDTO(
        UUID id,
        TipoMovimentacaoItem tipo,
        Instant dataMovimentacao,
        UUID instanciaItemId,
        String instanciaIdentificacao,
        UUID localDestinoId,
        String localDestinoNome,
        String observacao
) {}

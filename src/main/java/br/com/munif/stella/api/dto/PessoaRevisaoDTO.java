package br.com.munif.stella.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO que representa uma revisão auditada de uma pessoa.
 *
 * <p>Utilizado para expor o histórico de alterações rastreadas pelo Hibernate Envers.
 * Cada instância deste DTO corresponde a uma versão da entidade {@code Pessoa}
 * em um momento específico no tempo.</p>
 *
 * @param revisao        número sequencial da revisão gerado pelo Hibernate Envers
 * @param dataHora       data e hora em que a revisão foi criada (UTC)
 * @param tipo           tipo da operação que gerou a revisão (ex.: {@code "ADD"}, {@code "MOD"}, {@code "DEL"})
 * @param pessoa         snapshot dos dados da pessoa nesta revisão
 * @param camposAlterados lista de nomes dos campos que foram modificados nesta revisão;
 *                        vazia na criação (ADD), pois todos os campos são novos
 */
public record PessoaRevisaoDTO(
        Number revisao,
        Instant dataHora,
        String tipo,
        PessoaResponseDTO pessoa,
        List<String> camposAlterados
) {
}

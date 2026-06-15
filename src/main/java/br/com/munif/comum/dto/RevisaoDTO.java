package br.com.munif.comum.dto;

import java.time.Instant;

/**
 * DTO genérico que representa uma revisão auditada de uma entidade.
 *
 * <p>Utilizado para expor o histórico de alterações rastreadas pelo Hibernate Envers.
 * Cada instância corresponde a uma versão da entidade em um momento específico no tempo.
 * O parâmetro de tipo {@code T} deve ser o DTO de resposta completo da entidade auditada.</p>
 *
 * <p>Exemplo de uso: {@code RevisaoDTO<PessoaResponseDTO>} para o histórico de alterações
 * de uma {@code Pessoa}.</p>
 *
 * @param <T>      tipo do DTO de resposta da entidade auditada
 * @param revisao  número sequencial da revisão gerado pelo Hibernate Envers
 * @param dataHora data e hora em que a revisão foi criada (UTC)
 * @param entidade snapshot dos dados da entidade nesta revisão, representado como DTO
 */
public record RevisaoDTO<T>(
        Number revisao,
        Instant dataHora,
        T entidade
) {
}

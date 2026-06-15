package br.com.munif.stella.api.entity;

import br.com.munif.comum.persistencia.Entidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.Instant;

/**
 * Entidade que registra uma movimentação de uma instância de item entre locais de armazenamento.
 *
 * <p>Cada movimentação descreve um evento do ciclo de vida físico de uma {@link InstanciaItem}:
 * pode ser uma entrada (item chega ao estoque), uma saída (item sai do estoque) ou
 * uma transferência entre dois locais internos. O histórico de movimentações permite
 * rastrear onde um item esteve ao longo do tempo.</p>
 *
 * <p>A data da movimentação é automaticamente preenchida com o instante da persistência
 * caso não seja informada explicitamente.</p>
 *
 * <p>A entidade é auditada pelo Hibernate Envers: todas as alterações são registradas
 * na tabela {@code movimentacao_item_aud}.</p>
 */
@Entity
@Audited
@Table(name = "movimentacao_item")
@Getter
@Setter
@NoArgsConstructor
public class MovimentacaoItem extends Entidade {

    /**
     * Tipo da movimentação realizada.
     * Define a semântica do evento: {@link TipoMovimentacaoItem#ENTRADA}, {@link TipoMovimentacaoItem#SAIDA}
     * ou {@link TipoMovimentacaoItem#TRANSFERENCIA}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoMovimentacaoItem tipo;

    /**
     * Data e hora em que a movimentação ocorreu (com fuso UTC).
     * Preenchido automaticamente pelo callback {@link #prePersist()} caso não informado.
     */
    @Column(name = "data_movimentacao", nullable = false)
    private Instant dataMovimentacao;

    /**
     * Instância do item que foi movimentada.
     * Obrigatório — toda movimentação deve estar vinculada a uma instância.
     * Carregado de forma lazy para evitar joins desnecessários.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instancia_item_id", nullable = false)
    private InstanciaItem instanciaItem;

    /**
     * Local de origem da movimentação.
     * {@code null} em movimentações do tipo {@link TipoMovimentacaoItem#ENTRADA},
     * quando o item ainda não possuía localização no sistema.
     * Carregado de forma lazy para evitar joins desnecessários.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_origem_id")
    private LocalArmazenamento localOrigem;

    /**
     * Local de destino da movimentação.
     * {@code null} em movimentações do tipo {@link TipoMovimentacaoItem#SAIDA},
     * quando o item deixa o controle do estoque.
     * Carregado de forma lazy para evitar joins desnecessários.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_destino_id")
    private LocalArmazenamento localDestino;

    /**
     * Motivo resumido da movimentação (ex.: "Manutenção", "Redistribuição de equipamentos").
     * Até 200 caracteres.
     */
    @Column(name = "motivo", length = 200)
    private String motivo;

    /**
     * Observações complementares sobre a movimentação com mais detalhes ou contexto.
     * Até 1000 caracteres.
     */
    @Column(name = "observacao", length = 1000)
    private String observacao;

    /**
     * Callback JPA executado automaticamente antes da primeira persistência.
     *
     * <p>Garante que {@link #dataMovimentacao} nunca seja persistido como {@code null},
     * utilizando o instante atual (UTC) como valor padrão.</p>
     */
    @Override
    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (dataMovimentacao == null) {
            dataMovimentacao = Instant.now();
        }
    }
}

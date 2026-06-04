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

@Entity
@Audited
@Table(name = "movimentacao_item")
@Getter
@Setter
@NoArgsConstructor
public class MovimentacaoItem extends Entidade {

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoMovimentacaoItem tipo;

    @Column(name = "data_movimentacao", nullable = false)
    private Instant dataMovimentacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instancia_item_id", nullable = false)
    private InstanciaItem instanciaItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "local_destino_id", nullable = false)
    private LocalArmazenamento localDestino;

    @Column(name = "observacao", length = 1000)
    private String observacao;

    @Override
    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (dataMovimentacao == null) {
            dataMovimentacao = Instant.now();
        }
    }
}

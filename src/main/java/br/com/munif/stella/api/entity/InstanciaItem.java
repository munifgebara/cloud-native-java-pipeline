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

@Entity
@Audited
@Table(name = "instancia_item")
@Getter
@Setter
@NoArgsConstructor
public class InstanciaItem extends Entidade {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_mestre_id", nullable = false)
    private ItemMestre itemMestre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_armazenamento_id")
    private LocalArmazenamento localAtual;

    @Column(name = "identificador", length = 100)
    private String identificador;

    @Column(name = "patrimonio", length = 100)
    private String patrimonio;

    @Column(name = "numero_serie", length = 150)
    private String numeroSerie;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_operacional", nullable = false, length = 30)
    private StatusOperacionalInstancia statusOperacional = StatusOperacionalInstancia.DISPONIVEL;

    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    @Override
    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (statusOperacional == null) {
            statusOperacional = StatusOperacionalInstancia.DISPONIVEL;
        }
    }
}

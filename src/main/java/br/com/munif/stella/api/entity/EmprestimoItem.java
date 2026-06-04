package br.com.munif.stella.api.entity;

import br.com.munif.comum.persistencia.Entidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.time.LocalDate;

@Entity
@Audited
@Table(name = "emprestimo_item")
@Getter
@Setter
@NoArgsConstructor
public class EmprestimoItem extends Entidade {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instancia_item_id", nullable = false)
    private InstanciaItem instanciaItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

    @Column(name = "data_emprestimo", nullable = false)
    private Instant dataEmprestimo;

    @Column(name = "previsao_devolucao")
    private LocalDate previsaoDevolucao;

    @Column(name = "data_devolucao")
    private Instant dataDevolucao;

    @Column(name = "observacao", length = 1000)
    private String observacao;

    @Override
    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (dataEmprestimo == null) {
            dataEmprestimo = Instant.now();
        }
    }
}

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

/**
 * Entity that records a loan of an item instance to a person.
 *
 * <p>A loan links a {@link InstanciaItem physical instance} to a {@link Pessoa},
 * recording the date the item was lent and, optionally, the expected and actual
 * return dates. While the loan is active (without {@link #dataDevolucao}), the instance
 * has the status {@link StatusOperacionalInstancia#EMPRESTADO}.</p>
 *
 * <p>The return is recorded by filling in {@link #dataDevolucao}, without deleting
 * the record — ensuring historical traceability.</p>
 *
 * <p>This entity is audited by Hibernate Envers: all changes are recorded
 * in the {@code emprestimo_item_aud} table.</p>
 */
@Entity
@Audited
@Table(name = "emprestimo_item")
@Getter
@Setter
@NoArgsConstructor
public class EmprestimoItem extends Entidade {

    /**
     * Item instance that was loaned.
     * Required — every loan must reference an existing instance.
     * Loaded lazily to avoid unnecessary joins.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instancia_item_id", nullable = false)
    private InstanciaItem instanciaItem;

    /**
     * Person who received the loaned item (borrower).
     * Required — every loan must identify who is responsible for holding the item.
     * Loaded lazily to avoid unnecessary joins.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

    /**
     * Date and time the loan was made (UTC timezone).
     * Automatically populated by the {@link #prePersist()} callback if not provided.
     */
    @Column(name = "data_emprestimo", nullable = false)
    private Instant dataEmprestimo;

    /**
     * Expected return date of the item.
     * Represented as {@link LocalDate} (date only, no time) to facilitate
     * deadline comparisons and overdue alerts.
     * Optional — may be {@code null} when no deadline is defined.
     */
    @Column(name = "previsao_devolucao")
    private LocalDate previsaoDevolucao;

    /**
     * Date and time the item was actually returned (UTC timezone).
     * {@code null} indicates the loan is still open (item still with the person).
     * When populated, closes the loan cycle and releases the instance.
     */
    @Column(name = "data_devolucao")
    private Instant dataDevolucao;

    /**
     * Notes about the loan (item condition at checkout, agreed terms, etc.).
     * Up to 1000 characters.
     */
    @Column(name = "observacao", length = 1000)
    private String observacao;

    /**
     * JPA callback executed automatically before the first persistence.
     *
     * <p>Ensures that {@link #dataEmprestimo} is never persisted as {@code null},
     * using the current instant (UTC) as the default value.</p>
     */
    @Override
    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (dataEmprestimo == null) {
            dataEmprestimo = Instant.now();
        }
    }
}

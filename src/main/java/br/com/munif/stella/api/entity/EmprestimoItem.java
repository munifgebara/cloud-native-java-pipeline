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
 * Entidade que registra um empréstimo de uma instância de item a uma pessoa.
 *
 * <p>Um empréstimo vincula uma {@link InstanciaItem instância física} a uma {@link Pessoa},
 * registrando a data em que o item saiu e, opcionalmente, a previsão e data efetiva de
 * devolução. Enquanto o empréstimo estiver ativo (sem {@link #dataDevolucao}), a instância
 * fica com o status {@link StatusOperacionalInstancia#EMPRESTADO}.</p>
 *
 * <p>A devolução é registrada pelo preenchimento de {@link #dataDevolucao}, sem deletar
 * o registro — garantindo rastreabilidade histórica.</p>
 *
 * <p>A entidade é auditada pelo Hibernate Envers: todas as alterações são registradas
 * na tabela {@code emprestimo_item_aud}.</p>
 */
@Entity
@Audited
@Table(name = "emprestimo_item")
@Getter
@Setter
@NoArgsConstructor
public class EmprestimoItem extends Entidade {

    /**
     * Instância do item que foi emprestada.
     * Obrigatório — todo empréstimo deve referenciar uma instância existente.
     * Carregado de forma lazy para evitar joins desnecessários.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instancia_item_id", nullable = false)
    private InstanciaItem instanciaItem;

    /**
     * Pessoa que recebeu o item emprestado (tomador do empréstimo).
     * Obrigatório — todo empréstimo deve identificar o responsável pela posse do item.
     * Carregado de forma lazy para evitar joins desnecessários.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

    /**
     * Data e hora em que o empréstimo foi realizado (com fuso UTC).
     * Preenchido automaticamente pelo callback {@link #prePersist()} caso não informado.
     */
    @Column(name = "data_emprestimo", nullable = false)
    private Instant dataEmprestimo;

    /**
     * Data prevista para a devolução do item.
     * Representada como {@link LocalDate} (apenas data, sem horário) para facilitar
     * comparações de prazo e alertas de atraso.
     * Opcional — pode ser {@code null} quando não há prazo definido.
     */
    @Column(name = "previsao_devolucao")
    private LocalDate previsaoDevolucao;

    /**
     * Data e hora em que o item foi efetivamente devolvido (com fuso UTC).
     * {@code null} indica que o empréstimo está em aberto (item ainda com a pessoa).
     * Quando preenchido, encerra o ciclo do empréstimo e libera a instância.
     */
    @Column(name = "data_devolucao")
    private Instant dataDevolucao;

    /**
     * Observações sobre o empréstimo (estado do item na saída, condições acordadas, etc.).
     * Até 1000 caracteres.
     */
    @Column(name = "observacao", length = 1000)
    private String observacao;

    /**
     * Callback JPA executado automaticamente antes da primeira persistência.
     *
     * <p>Garante que {@link #dataEmprestimo} nunca seja persistido como {@code null},
     * utilizando o instante atual (UTC) como valor padrão.</p>
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

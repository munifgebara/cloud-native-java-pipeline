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

/**
 * Entidade que representa uma instância física de um {@link ItemMestre}.
 *
 * <p>Enquanto o {@link ItemMestre} descreve o "modelo" do bem (ex.: "Notebook Dell"),
 * a instância representa um exemplar concreto e único desse modelo, identificado por
 * patrimônio, número de série ou um código interno. Cada instância possui seu próprio
 * status operacional e localização atual.</p>
 *
 * <p>Exemplo: o item mestre "Projetor Epson" pode ter três instâncias distintas,
 * uma em cada sala de reunião da empresa.</p>
 *
 * <p>A entidade é auditada pelo Hibernate Envers: todas as alterações são registradas
 * na tabela {@code instancia_item_aud}.</p>
 */
@Entity
@Audited
@Table(name = "instancia_item")
@Getter
@Setter
@NoArgsConstructor
public class InstanciaItem extends Entidade {

    /**
     * Item mestre ao qual esta instância pertence.
     * Relacionamento obrigatório — toda instância deve referenciar um item mestre válido.
     * Carregado de forma lazy para evitar joins desnecessários.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_mestre_id", nullable = false)
    private ItemMestre itemMestre;

    /**
     * Local de armazenamento onde a instância está fisicamente localizada no momento.
     * Pode ser {@code null} quando o local ainda não foi definido ou o item está em trânsito.
     * Carregado de forma lazy para evitar joins desnecessários.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_armazenamento_id")
    private LocalArmazenamento localAtual;

    /**
     * Código de identificação interno da instância, definido livremente pela organização.
     * Alternativa ao {@link #patrimonio} e ao {@link #numeroSerie} para identificação rápida.
     * Até 100 caracteres.
     */
    @Column(name = "identificador", length = 100)
    private String identificador;

    /**
     * Número de patrimônio atribuído ao bem pela organização.
     * Geralmente segue uma numeração sequencial controlada pelo setor de patrimônio.
     * Até 100 caracteres.
     */
    @Column(name = "patrimonio", length = 100)
    private String patrimonio;

    /**
     * Número de série do fabricante, gravado fisicamente no equipamento.
     * Utilizado para identificação junto ao fabricante e para fins de garantia.
     * Até 150 caracteres.
     */
    @Column(name = "numero_serie", length = 150)
    private String numeroSerie;

    /**
     * Status operacional atual da instância, controlado pelo fluxo de movimentação e empréstimo.
     * Inicializado com {@link StatusOperacionalInstancia#DISPONIVEL} na criação.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_operacional", nullable = false, length = 30)
    private StatusOperacionalInstancia statusOperacional = StatusOperacionalInstancia.DISPONIVEL;

    /**
     * Observações internas sobre esta instância específica
     * (ex.: "Tela com risco na lateral", "Adaptador de fonte ausente").
     * Até 1000 caracteres.
     */
    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    /**
     * Indica a origem do cadastro desta instância (ex.: {@code "MANUAL"}, {@code "FOTO"}, {@code "IA"}).
     * Permite rastrear como a instância foi inserida no sistema.
     * Até 50 caracteres.
     */
    @Column(name = "origem_cadastro", length = 50)
    private String origemCadastro;

    /**
     * Callback JPA executado automaticamente antes da primeira persistência.
     *
     * <p>Garante que o {@link #statusOperacional} nunca seja persistido como {@code null},
     * aplicando o valor padrão {@link StatusOperacionalInstancia#DISPONIVEL} caso necessário.</p>
     */
    @Override
    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (statusOperacional == null) {
            statusOperacional = StatusOperacionalInstancia.DISPONIVEL;
        }
    }
}

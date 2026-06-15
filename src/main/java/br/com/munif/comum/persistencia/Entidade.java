package br.com.munif.comum.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Classe base mapeada para todas as entidades persistentes do sistema.
 *
 * <p>Centraliza campos de infraestrutura comuns — identidade, ciclo de vida,
 * exclusão lógica e controle de concorrência — de modo que as subclasses
 * concentrem-se apenas nas regras de negócio.</p>
 *
 * <h2>Exclusão lógica</h2>
 * <p>Nenhuma entidade é removida fisicamente do banco de dados.
 * O campo {@code ativo} é usado para indicar se o registro está em uso
 * ({@code true}) ou foi inativado ({@code false}).
 * Para inativar, chame {@link #excluirLogicamente()}.</p>
 *
 * <h2>Controle de concorrência</h2>
 * <p>O campo {@code version} implementa o mecanismo de {@code @Version} do JPA,
 * prevenindo atualizações simultâneas conflitantes
 * (optimistic locking).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@MappedSuperclass
public abstract class Entidade implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Identificador único da entidade, gerado automaticamente pelo banco de dados (UUID v4).
     * Nunca é alterado após a criação do registro.
     */
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Indica se o registro está ativo ({@code true}) ou foi inativado logicamente ({@code false}).
     * Toda entidade nasce ativa. Use {@link #excluirLogicamente()} para inativar.
     */
    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    /**
     * Instante em que o registro foi criado. Preenchido automaticamente em {@link #prePersist()}
     * e nunca alterado.
     */
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    /**
     * Instante da última alteração do registro. Atualizado automaticamente em
     * {@link #prePersist()} e {@link #preUpdate()}.
     */
    @Column(name = "alterado_em", nullable = false)
    private Instant alteradoEm;

    /**
     * Versão do registro para controle de concorrência otimista (optimistic locking).
     * O JPA incrementa este valor a cada {@code UPDATE}, evitando que duas transações
     * simultâneas sobrescrevam alterações uma da outra sem perceber.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Campo de extensão genérico para anotações livres.
     * Incluído na estrutura base para permitir que as subclasses armazenem
     * informações textuais curtas sem necessitar de nova migração de banco.
     */
    @Column(name = "extra", length = 200)
    private String extra;

    /**
     * Campo reservado para uso interno e extensibilidade futura.
     * Presente em todas as tabelas por herdar desta superclasse.
     */
    @Column(name = "oi", length = 100)
    private String oi;

    /**
     * Callback do JPA executado imediatamente antes da primeira persistência do registro.
     *
     * <p>Responsabilidades:</p>
     * <ul>
     *   <li>Registrar os instantes {@code criadoEm} e {@code alteradoEm}.</li>
     *   <li>Garantir que a entidade inicie sempre no estado ativo — mesmo que o
     *       chamador tenha alterado o campo {@code ativo} antes de chamar {@code save()},
     *       pois a inativação deve ocorrer em uma operação dedicada após a criação.</li>
     * </ul>
     */
    @PrePersist
    public void prePersist() {
        Instant agora = Instant.now();
        this.criadoEm = agora;
        this.alteradoEm = agora;
        // Garante estado ativo na criação; inativação é feita após a persistência inicial.
        this.ativo = true;
    }

    /**
     * Callback do JPA executado imediatamente antes de cada atualização do registro.
     * Mantém {@code alteradoEm} sincronizado com o instante real da modificação.
     */
    @PreUpdate
    public void preUpdate() {
        this.alteradoEm = Instant.now();
    }

    /**
     * Retorna {@code true} quando a entidade ainda não foi persistida (sem {@code id} atribuído).
     *
     * @return {@code true} se o registro é novo; {@code false} caso já exista no banco.
     */
    public boolean isNova() {
        return this.id == null;
    }

    /**
     * Inativa logicamente o registro, definindo {@code ativo = false}.
     * O registro permanece no banco de dados e pode ser consultado por consultas
     * que incluam inativas, mas não aparece nas listagens padrão.
     */
    public void excluirLogicamente() {
        this.ativo = false;
    }

    /**
     * Reativa um registro previamente inativado, definindo {@code ativo = true}.
     */
    public void reativar() {
        this.ativo = true;
    }
}

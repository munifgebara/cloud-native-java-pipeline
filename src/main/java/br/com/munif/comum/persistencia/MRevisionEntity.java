package br.com.munif.comum.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Entidade de revisão personalizada para o Hibernate Envers.
 *
 * <p>Cada vez que um registro auditado (anotado com {@code @Audited}) é criado,
 * alterado ou excluído, o Envers insere uma linha nesta tabela ({@code versao})
 * representando aquele momento no tempo. As tabelas de auditoria ({@code _aud})
 * referenciam esta revisão pela coluna {@code rev}.</p>
 *
 * <p>Os campos {@code ip} e {@code usuario} são preenchidos pelo
 * {@link MRevisionEntityListener} antes de cada nova revisão.</p>
 *
 * @see MRevisionEntityListener
 */
@Getter
@Setter
@Entity
@Table(name = "versao")
@RevisionEntity(MRevisionEntityListener.class)
public class MRevisionEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Número sequencial da revisão, gerado pelo banco de dados.
     * Utilizado como chave estrangeira nas tabelas de auditoria ({@code _aud}).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * Instante exato em que a revisão foi registrada.
     * Preenchido automaticamente pelo Envers.
     */
    @RevisionTimestamp
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    /**
     * Endereço IP do cliente que originou a operação auditada.
     * Preenchido pelo {@link MRevisionEntityListener}.
     */
    @Column(name = "ip", length = 45)
    private String ip;

    /**
     * Login do usuário responsável pela alteração auditada.
     * Preenchido pelo {@link MRevisionEntityListener}.
     */
    @Column(name = "usuario", length = 100)
    private String usuario;

    /**
     * Campo reservado para extensibilidade futura.
     */
    @Column(name = "oi", length = 100)
    private String oi;
}
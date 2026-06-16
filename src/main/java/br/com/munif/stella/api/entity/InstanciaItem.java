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
 * Entity representing a physical instance of an {@link ItemMestre}.
 *
 * <p>While {@link ItemMestre} describes the "model" of the asset (e.g.: "Dell Laptop"),
 * the instance represents a concrete, unique unit of that model, identified by
 * asset tag, serial number, or an internal code. Each instance has its own
 * operational status and current location.</p>
 *
 * <p>Example: the main item "Epson Projector" can have three distinct instances,
 * one in each meeting room of the company.</p>
 *
 * <p>This entity is audited by Hibernate Envers: all changes are recorded
 * in the {@code instancia_item_aud} table.</p>
 */
@Entity
@Audited
@Table(name = "instancia_item")
@Getter
@Setter
@NoArgsConstructor
public class InstanciaItem extends Entidade {

    /**
     * Main item to which this instance belongs.
     * Required relationship — every instance must reference a valid main item.
     * Loaded lazily to avoid unnecessary joins.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_mestre_id", nullable = false)
    private ItemMestre itemMestre;

    /**
     * Storage location where the instance is currently physically located.
     * May be {@code null} when the location has not yet been defined or the item is in transit.
     * Loaded lazily to avoid unnecessary joins.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_armazenamento_id")
    private LocalArmazenamento localAtual;

    /**
     * Internal identification code of the instance, freely defined by the organization.
     * Alternative to {@link #patrimonio} and {@link #numeroSerie} for quick identification.
     * Up to 100 characters.
     */
    @Column(name = "identificador", length = 100)
    private String identificador;

    /**
     * Asset tag number assigned to the asset by the organization.
     * Usually follows a sequential numbering controlled by the asset management department.
     * Up to 100 characters.
     */
    @Column(name = "patrimonio", length = 100)
    private String patrimonio;

    /**
     * Manufacturer serial number, physically engraved on the equipment.
     * Used for identification with the manufacturer and for warranty purposes.
     * Up to 150 characters.
     */
    @Column(name = "numero_serie", length = 150)
    private String numeroSerie;

    /**
     * Current operational status of the instance, controlled by the movement and loan flow.
     * Initialized with {@link StatusOperacionalInstancia#DISPONIVEL} upon creation.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_operacional", nullable = false, length = 30)
    private StatusOperacionalInstancia statusOperacional = StatusOperacionalInstancia.DISPONIVEL;

    /**
     * Internal notes about this specific instance
     * (e.g.: "Screen with scratch on the side", "Power adapter missing").
     * Up to 1000 characters.
     */
    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    /**
     * Indicates the registration origin of this instance (e.g.: {@code "MANUAL"}, {@code "FOTO"}, {@code "IA"}).
     * Allows tracking how the instance was entered into the system.
     * Up to 50 characters.
     */
    @Column(name = "origem_cadastro", length = 50)
    private String origemCadastro;

    /**
     * JPA callback executed automatically before the first persistence.
     *
     * <p>Ensures that {@link #statusOperacional} is never persisted as {@code null},
     * applying the default value {@link StatusOperacionalInstancia#DISPONIVEL} if necessary.</p>
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

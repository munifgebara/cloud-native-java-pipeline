package br.com.stella.api.entity;

import br.com.munif.common.persistencia.BaseEntity;
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
 * Entity that records a movement of an item instance between storage locations.
 *
 * <p>Each movement describes an event in the physical lifecycle of an {@link ItemInstance}:
 * it can be an inbound (item arrives in stock), an outbound (item leaves stock), or
 * a transfer between two internal locations. The movement history allows
 * tracking where an item has been over time.</p>
 *
 * <p>The movement date is automatically populated with the persistence instant
 * if not explicitly provided.</p>
 *
 * <p>This entity is audited by Hibernate Envers: all changes are recorded
 * in the {@code movimentacao_item_aud} table.</p>
 */
@Entity
@Audited
@Table(name = "movimentacao_item")
@Getter
@Setter
@NoArgsConstructor
public class ItemMovement extends BaseEntity {

    /**
     * Type of movement performed.
     * Defines the semantics of the event: {@link ItemMovementType#ENTRADA}, {@link ItemMovementType#SAIDA}
     * or {@link ItemMovementType#TRANSFERENCIA}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private ItemMovementType type;

    /**
     * Date and time the movement occurred (UTC timezone).
     * Automatically populated by the {@link #prePersist()} callback if not provided.
     */
    @Column(name = "data_movimentacao", nullable = false)
    private Instant dataMovimentacao;

    /**
     * Item instance that was moved.
     * Required — every movement must be linked to an instance.
     * Loaded lazily to avoid unnecessary joins.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instancia_item_id", nullable = false)
    private ItemInstance itemInstance;

    /**
     * Origin location of the movement.
     * {@code null} for movements of type {@link ItemMovementType#ENTRADA},
     * when the item had no location in the system yet.
     * Loaded lazily to avoid unnecessary joins.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_origem_id")
    private StorageLocation originLocation;

    /**
     * Destination location of the movement.
     * {@code null} for movements of type {@link ItemMovementType#SAIDA},
     * when the item leaves inventory control.
     * Loaded lazily to avoid unnecessary joins.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_destino_id")
    private StorageLocation destinationLocation;

    /**
     * Brief reason for the movement (e.g.: "Maintenance", "Equipment redistribution").
     * Up to 200 characters.
     */
    @Column(name = "reason", length = 200)
    private String reason;

    /**
     * Additional notes about the movement with more details or context.
     * Up to 1000 characters.
     */
    @Column(name = "notes", length = 1000)
    private String notes;

    /**
     * JPA callback executed automatically before the first persistence.
     *
     * <p>Ensures that {@link #dataMovimentacao} is never persisted as {@code null},
     * using the current instant (UTC) as the default value.</p>
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

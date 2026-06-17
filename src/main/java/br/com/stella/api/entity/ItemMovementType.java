package br.com.stella.api.entity;

/**
 * Enumeration that classifies the type of a {@link ItemMovement}.
 *
 * <p>The type determines the semantics of the movement and influences how the origin
 * and destination locations are interpreted: an inbound has no origin; an outbound has no destination;
 * a transfer has both.</p>
 */
public enum ItemMovementType {

    /**
     * The item enters the inventory control system.
     * Typically occurs at the initial registration of the instance or upon receipt of an asset.
     * Has no origin location — only a destination.
     */
    ENTRADA,

    /**
     * The item leaves the inventory control system.
     * Occurs when the item is discarded, returned to the supplier, or permanently removed.
     * Has no destination location — only an origin.
     */
    SAIDA,

    /**
     * The item is transferred from one storage location to another within the system.
     * Has both an origin location and a destination location.
     */
    TRANSFERENCIA
}

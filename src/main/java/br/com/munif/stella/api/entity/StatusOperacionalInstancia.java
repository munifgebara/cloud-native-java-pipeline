package br.com.munif.stella.api.entity;

/**
 * Enumeration representing the possible operational states of an {@link InstanciaItem}.
 *
 * <p>The status controls what can be done with each instance: only
 * {@link #DISPONIVEL} instances can be loaned or moved. Status transitions
 * are managed by the movement and loan services.</p>
 */
public enum StatusOperacionalInstancia {

    /**
     * The instance is available for use, loan, or movement.
     * This is the default state after registration.
     */
    DISPONIVEL,

    /**
     * The instance is in the process of being moved between storage locations.
     * In this state, the item cannot be loaned until the movement is complete.
     */
    EM_MOVIMENTACAO,

    /**
     * The instance has been loaned to a {@link Pessoa} and is in their possession.
     * Remains in this state until the return is registered.
     */
    EMPRESTADO,

    /**
     * The instance has been deactivated and is no longer in operational use.
     * May indicate items that have been discarded, awaiting maintenance, or permanently out of service.
     */
    INATIVO
}

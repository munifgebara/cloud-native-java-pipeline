package br.com.munif.stella.api.service;

import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;

/**
 * Isolated business rules for validating the state of an {@link InstanciaItem}.
 *
 * <p>This class groups verifications that depend only on the internal data of the instance,
 * without access to repositories or other services. Keeping them separate from
 * {@link InstanciaItemService} makes unit testing easier and makes the rules
 * easier to locate and maintain.</p>
 *
 * <p>Package visibility — this class is not part of the public API of the system.</p>
 */
final class InstanciaItemRegras {

    private InstanciaItemRegras() {
        // Stateless utility — not instantiable
    }

    /**
     * Validates the consistency between the operational status of the instance and the presence of a current location.
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>{@code DISPONIVEL} → requires a current location (the item is stored somewhere).</li>
     *   <li>{@code EMPRESTADO} → must not have a current location (the item is with a responsible party).</li>
     *   <li>{@code EM_MOVIMENTACAO} → must not have a current location (it is in transit).</li>
     *   <li>{@code INATIVO} → must not have a current location (it has been removed from inventory).</li>
     * </ul>
     *
     * @param instancia instance to validate
     * @throws IllegalArgumentException if the status and location are incompatible
     */
    static void validarCoerenciaStatusLocal(InstanciaItem instancia) {
        StatusOperacionalInstancia status = instancia.getStatusOperacional();

        if (status == StatusOperacionalInstancia.DISPONIVEL && instancia.getLocalAtual() == null) {
            throw new IllegalArgumentException("Available instance must have a current location.");
        }
        if (status == StatusOperacionalInstancia.EMPRESTADO && instancia.getLocalAtual() != null) {
            throw new IllegalArgumentException("Loaned instance must not have a current location.");
        }
        if (status == StatusOperacionalInstancia.EM_MOVIMENTACAO && instancia.getLocalAtual() != null) {
            throw new IllegalArgumentException("Instance in transit must not have a current location.");
        }
        if (status == StatusOperacionalInstancia.INATIVO && instancia.getLocalAtual() != null) {
            throw new IllegalArgumentException("Inactive instance must not have a current location.");
        }
    }

    /**
     * Ensures the instance is active, available, and has a current location.
     *
     * <p>Used before operations that require the item to be physically
     * present and ready for use, such as loans and transfers.</p>
     *
     * @param instancia                the instance to check
     * @param mensagemInstanciaInativa error message if the instance is inactive
     * @param mensagemStatusInvalido   error message if the status is not {@code DISPONIVEL}
     * @param mensagemLocalAusente     error message if there is no current location
     * @throws IllegalArgumentException if any of the conditions is not met
     */
    static void exigirDisponivelComLocal(
            InstanciaItem instancia,
            String mensagemInstanciaInativa,
            String mensagemStatusInvalido,
            String mensagemLocalAusente
    ) {
        if (!instancia.isAtivo()) {
            throw new IllegalArgumentException(mensagemInstanciaInativa);
        }
        if (instancia.getStatusOperacional() != StatusOperacionalInstancia.DISPONIVEL) {
            throw new IllegalArgumentException(mensagemStatusInvalido);
        }
        if (instancia.getLocalAtual() == null) {
            throw new IllegalArgumentException(mensagemLocalAusente);
        }
    }

    /**
     * Ensures the instance has status {@code EMPRESTADO}.
     *
     * <p>Used before registering the return of a loaned item.</p>
     *
     * @param instancia              the instance to check
     * @param mensagemStatusInvalido error message if the status is not {@code EMPRESTADO}
     * @throws IllegalArgumentException if the instance is not loaned
     */
    static void exigirEmprestada(InstanciaItem instancia, String mensagemStatusInvalido) {
        if (instancia.getStatusOperacional() != StatusOperacionalInstancia.EMPRESTADO) {
            throw new IllegalArgumentException(mensagemStatusInvalido);
        }
    }
}

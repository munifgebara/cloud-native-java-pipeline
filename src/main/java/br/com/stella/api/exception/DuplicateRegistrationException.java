package br.com.stella.api.exception;

/**
 * Exception thrown when attempting to register a record with a unique key that already exists.
 *
 * <p>Examples:</p>
 * <ul>
 *   <li>Attempting to register a person with a CPF/CNPJ that already exists.</li>
 *   <li>Attempting to create a user with a duplicate email or login.</li>
 * </ul>
 *
 * <p>Should be handled by {@code GlobalExceptionHandler} and mapped to
 * HTTP 409 (Conflict).</p>
 */
public class DuplicateRegistrationException extends RuntimeException {

    /**
     * Creates a new exception with a message describing which field is duplicated.
     *
     * @param message description of the conflict (e.g., "CPF 123.456.789-00 already registered.")
     */
    public DuplicateRegistrationException(String message) {
        super(message);
    }
}

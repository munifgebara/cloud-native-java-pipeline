package br.com.munif.common.utils.validacoes;

import java.util.regex.Pattern;

/**
 * Validation and formatting utilities for data typical of the Brazilian context.
 *
 * <p>All methods are static and stateless — this class must not be instantiated.
 * Attempting to instantiate it will throw an {@link UnsupportedOperationException}.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Validate a CPF received from the user
 * if (!BrValidations.validarCPF(cpfDoFormulario)) {
 *     throw new IllegalArgumentException("Invalid CPF.");
 * }
 *
 * // Normalize text before persisting
 * String nomeLimpo = BrValidations.trimToNull(nomeDoFormulario);
 * }</pre>
 *
 * <h2>Normalization conventions</h2>
 * <ul>
 *   <li>CPF and CNPJ are stored with digits only (no dots, dashes, or slashes).</li>
 *   <li>Phone numbers are stored with digits only (no parentheses, spaces, or dashes).</li>
 *   <li>CEP (postal code) is stored with digits only (no dash).</li>
 *   <li>Email is stored in lowercase.</li>
 * </ul>
 */
public final class BrValidations {

    /** Regular expression for basic email validation. */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    /** Pattern for Brazilian mobile phone: 11 digits (area code + 9 + number). */
    private static final Pattern MOBILE_PHONE_PATTERN =
            Pattern.compile("^\\d{11}$");

    /** Pattern for Brazilian landline: 10 digits (area code + number). */
    private static final Pattern LANDLINE_PHONE_PATTERN =
            Pattern.compile("^\\d{10}$");

    /** Pattern for Brazilian CEP (postal code): exactly 8 digits. */
    private static final Pattern CEP_PATTERN =
            Pattern.compile("^\\d{8}$");

    private BrValidations() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    // -------------------------------------------------------------------------
    // String normalization
    // -------------------------------------------------------------------------

    /**
     * Removes all non-numeric characters from a string.
     *
     * <p>Useful for normalizing CPF, CNPJ, phone number, and CEP before validating or persisting.</p>
     *
     * @param valor input string (may contain formatting such as dots, dashes, etc.)
     * @return string containing only digits, or {@code null} if the input is {@code null}
     */
    public static String somenteDigitos(String valor) {
        if (valor == null) {
            return null;
        }
        return valor.replaceAll("\\D", "");
    }

    /**
     * Trims whitespace from both ends of a string and returns {@code null} if the result
     * is empty.
     *
     * <p>Commonly used to normalize text fields before persisting, avoiding
     * empty strings in the database.</p>
     *
     * @param valor input string
     * @return string without leading/trailing spaces, or {@code null} if it is {@code null} or blank
     */
    public static String trimToNull(String valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim();
        return texto.isBlank() ? null : texto;
    }

    /**
     * Returns {@code true} if the string is {@code null} or contains only whitespace.
     *
     * @param valor string to check
     * @return {@code true} if empty or null; {@code false} otherwise
     */
    public static boolean isBlank(String valor) {
        return trimToNull(valor) == null;
    }

    /**
     * Returns {@code true} if the string contains at least one non-whitespace character.
     *
     * @param valor string to check
     * @return {@code true} if it has content; {@code false} if null or blank
     */
    public static boolean isNotBlank(String valor) {
        return !isBlank(valor);
    }

    // -------------------------------------------------------------------------
    // CPF and CNPJ validation
    // -------------------------------------------------------------------------

    /**
     * Validates a CPF using the official check-digit algorithm.
     *
     * <p>The input may be formatted (e.g.: {@code "123.456.789-09"}) or unformatted
     * (e.g.: {@code "12345678909"}). Formatting is automatically removed before validation.</p>
     *
     * <p>CPFs with all identical digits (e.g.: {@code "111.111.111-11"}) are rejected,
     * as they would mathematically pass the check-digit calculation but are invalid.</p>
     *
     * @param cpf CPF to validate, with or without formatting
     * @return {@code true} if the CPF is mathematically valid; {@code false} otherwise
     */
    public static boolean validarCPF(String cpf) {
        String valor = somenteDigitos(cpf);

        if (valor == null || valor.length() != 11) {
            return false;
        }
        if (todosDigitosIguais(valor)) {
            return false;
        }

        int dv1 = computeCpfCheckDigit(valor.substring(0, 9), 10);
        int dv2 = computeCpfCheckDigit(valor.substring(0, 9) + dv1, 11);

        return valor.equals(valor.substring(0, 9) + dv1 + dv2);
    }

    /**
     * Validates a CNPJ using the official check-digit algorithm.
     *
     * <p>The input may be formatted (e.g.: {@code "12.345.678/0001-95"}) or unformatted.
     * CNPJs with all identical digits are rejected.</p>
     *
     * @param cnpj CNPJ to validate, with or without formatting
     * @return {@code true} if the CNPJ is mathematically valid; {@code false} otherwise
     */
    public static boolean validarCNPJ(String cnpj) {
        String valor = somenteDigitos(cnpj);

        if (valor == null || valor.length() != 14) {
            return false;
        }
        if (todosDigitosIguais(valor)) {
            return false;
        }

        int dv1 = computeCnpjCheckDigit(valor.substring(0, 12), new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        int dv2 = computeCnpjCheckDigit(valor.substring(0, 12) + dv1, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});

        return valor.equals(valor.substring(0, 12) + dv1 + dv2);
    }

    // -------------------------------------------------------------------------
    // Phone number validation
    // -------------------------------------------------------------------------

    /**
     * Validates a Brazilian phone number (mobile or landline).
     *
     * <p>Requirements:</p>
     * <ul>
     *   <li>10 digits for landline (area code + 8 digits) or 11 digits for mobile (area code + 9 + 8 digits).</li>
     *   <li>Area code must be a valid code defined by Anatel.</li>
     *   <li>Mobile must start with digit 9 after the area code.</li>
     * </ul>
     *
     * @param phone number to validate, with or without formatting
     * @return {@code true} if it is a valid Brazilian phone number; {@code false} otherwise
     */
    public static boolean validateBrazilianPhone(String phone) {
        String valor = somenteDigitos(phone);

        if (valor == null) {
            return false;
        }
        if (!LANDLINE_PHONE_PATTERN.matcher(valor).matches()
                && !MOBILE_PHONE_PATTERN.matcher(valor).matches()) {
            return false;
        }
        if (!validarDDD(valor.substring(0, 2))) {
            return false;
        }
        if (valor.length() == 11) {
            return valor.charAt(2) == '9';
        }
        return true;
    }

    /**
     * Validates exclusively a Brazilian mobile phone number (11 digits, starting with 9 after the area code).
     *
     * @param phone number to validate, with or without formatting
     * @return {@code true} if it is a valid mobile number; {@code false} otherwise
     */
    public static boolean validarCelularBR(String phone) {
        String valor = somenteDigitos(phone);

        if (valor == null || !MOBILE_PHONE_PATTERN.matcher(valor).matches()) {
            return false;
        }
        return validarDDD(valor.substring(0, 2)) && valor.charAt(2) == '9';
    }

    /**
     * Validates exclusively a Brazilian landline phone number (10 digits).
     *
     * @param phone number to validate, with or without formatting
     * @return {@code true} if it is a valid landline; {@code false} otherwise
     */
    public static boolean validateBrazilianLandline(String phone) {
        String valor = somenteDigitos(phone);

        if (valor == null || !LANDLINE_PHONE_PATTERN.matcher(valor).matches()) {
            return false;
        }
        return validarDDD(valor.substring(0, 2));
    }

    // -------------------------------------------------------------------------
    // CEP and email validation
    // -------------------------------------------------------------------------

    /**
     * Validates a Brazilian postal code (CEP), which must contain exactly 8 digits.
     *
     * <p>The input may be formatted with a dash (e.g.: {@code "01310-100"}) or unformatted.</p>
     *
     * @param zipCode CEP to validate
     * @return {@code true} if it has exactly 8 digits; {@code false} otherwise
     */
    public static boolean validarCEP(String zipCode) {
        String valor = somenteDigitos(zipCode);
        return valor != null && CEP_PATTERN.matcher(valor).matches();
    }

    /**
     * Validates an email address using a basic regular expression.
     *
     * @param email email to validate
     * @return {@code true} if the format is valid; {@code false} otherwise
     */
    public static boolean validarEmail(String email) {
        String valor = trimToNull(email);
        return valor != null && EMAIL_PATTERN.matcher(valor).matches();
    }

    // -------------------------------------------------------------------------
    // Area code (DDD) validation
    // -------------------------------------------------------------------------

    /**
     * Checks whether an area code (DDD) is valid according to Anatel's table.
     *
     * @param ddd two-digit area code, with or without formatting
     * @return {@code true} if it is a valid area code; {@code false} otherwise
     */
    public static boolean validarDDD(String ddd) {
        String valor = somenteDigitos(ddd);

        if (valor == null || valor.length() != 2) {
            return false;
        }

        int numero = Integer.parseInt(valor);
        return switch (numero) {
            case 11, 12, 13, 14, 15, 16, 17, 18, 19,
                 21, 22, 24, 27, 28,
                 31, 32, 33, 34, 35, 37, 38,
                 41, 42, 43, 44, 45, 46, 47, 48, 49,
                 51, 53, 54, 55,
                 61, 62, 63, 64, 65, 66, 67, 68, 69,
                 71, 73, 74, 75, 77, 79,
                 81, 82, 83, 84, 85, 86, 87, 88, 89,
                 91, 92, 93, 94, 95, 96, 97, 98, 99 -> true;
            default -> false;
        };
    }

    // -------------------------------------------------------------------------
    // Formatting
    // -------------------------------------------------------------------------

    /**
     * Formats a CPF in the {@code NNN.NNN.NNN-DD} pattern.
     *
     * @param cpf CPF with exactly 11 digits (with or without formatting)
     * @return formatted CPF
     * @throws IllegalArgumentException if the CPF does not have 11 digits after removing non-digits
     */
    public static String formatarCPF(String cpf) {
        String valor = somenteDigitos(cpf);

        if (valor == null || valor.length() != 11) {
            throw new IllegalArgumentException("CPF must contain 11 digits.");
        }
        return valor.substring(0, 3) + "."
                + valor.substring(3, 6) + "."
                + valor.substring(6, 9) + "-"
                + valor.substring(9, 11);
    }

    /**
     * Formats a CNPJ in the {@code NN.NNN.NNN/NNNN-DD} pattern.
     *
     * @param cnpj CNPJ with exactly 14 digits (with or without formatting)
     * @return formatted CNPJ
     * @throws IllegalArgumentException if the CNPJ does not have 14 digits after removing non-digits
     */
    public static String formatarCNPJ(String cnpj) {
        String valor = somenteDigitos(cnpj);

        if (valor == null || valor.length() != 14) {
            throw new IllegalArgumentException("CNPJ must contain 14 digits.");
        }
        return valor.substring(0, 2) + "."
                + valor.substring(2, 5) + "."
                + valor.substring(5, 8) + "/"
                + valor.substring(8, 12) + "-"
                + valor.substring(12, 14);
    }

    /**
     * Formats a CEP in the {@code NNNNN-NNN} pattern.
     *
     * @param zipCode CEP with exactly 8 digits (with or without formatting)
     * @return formatted CEP
     * @throws IllegalArgumentException if the CEP does not have 8 digits after removing non-digits
     */
    public static String formatarCEP(String zipCode) {
        String valor = somenteDigitos(zipCode);

        if (valor == null || valor.length() != 8) {
            throw new IllegalArgumentException("ZIP code must contain 8 digits.");
        }
        return valor.substring(0, 5) + "-" + valor.substring(5, 8);
    }

    /**
     * Formats a Brazilian phone number in the {@code (DD) NNNNN-NNNN} pattern (mobile)
     * or {@code (DD) NNNN-NNNN} (landline).
     *
     * @param phone phone number with 10 or 11 digits (with or without formatting)
     * @return formatted phone number
     * @throws IllegalArgumentException if the phone number does not have 10 or 11 digits
     */
    public static String formatBrazilianPhone(String phone) {
        String valor = somenteDigitos(phone);

        if (valor == null) {
            throw new IllegalArgumentException("Invalid phone number.");
        }
        if (valor.length() == 11) {
            return "(" + valor.substring(0, 2) + ") "
                    + valor.substring(2, 7) + "-"
                    + valor.substring(7, 11);
        }
        if (valor.length() == 10) {
            return "(" + valor.substring(0, 2) + ") "
                    + valor.substring(2, 6) + "-"
                    + valor.substring(6, 10);
        }
        throw new IllegalArgumentException("Phone number must contain 10 or 11 digits.");
    }

    // -------------------------------------------------------------------------
    // Validation methods with exception ("require")
    // -------------------------------------------------------------------------

    /**
     * Throws {@link IllegalArgumentException} if the provided CPF is invalid.
     *
     * @param cpf       CPF to validate
     * @param fieldName field name to include in the error message (e.g.: {@code "CPF"})
     * @throws IllegalArgumentException if the CPF is not valid
     */
    public static void exigirCPFValido(String cpf, String fieldName) {
        if (!validarCPF(cpf)) {
            throw new IllegalArgumentException(fieldName + " is invalid.");
        }
    }

    /**
     * Throws {@link IllegalArgumentException} if the provided CNPJ is invalid.
     *
     * @param cnpj      CNPJ to validate
     * @param fieldName field name to include in the error message (e.g.: {@code "CNPJ"})
     * @throws IllegalArgumentException if the CNPJ is not valid
     */
    public static void exigirCNPJValido(String cnpj, String fieldName) {
        if (!validarCNPJ(cnpj)) {
            throw new IllegalArgumentException(fieldName + " is invalid.");
        }
    }

    /**
     * Throws {@link IllegalArgumentException} if the provided phone number is invalid.
     *
     * @param phone  phone number to validate
     * @param fieldName field name to include in the error message
     * @throws IllegalArgumentException if the phone number is not valid
     */
    public static void requireValidPhone(String phone, String fieldName) {
        if (!validateBrazilianPhone(phone)) {
            throw new IllegalArgumentException(fieldName + " is invalid.");
        }
    }

    /**
     * Throws {@link IllegalArgumentException} if the provided CEP is invalid.
     *
     * @param zipCode       CEP to validate
     * @param fieldName field name to include in the error message
     * @throws IllegalArgumentException if the CEP is not valid
     */
    public static void exigirCEPValido(String zipCode, String fieldName) {
        if (!validarCEP(zipCode)) {
            throw new IllegalArgumentException(fieldName + " is invalid.");
        }
    }

    /**
     * Throws {@link IllegalArgumentException} if the provided email is invalid.
     *
     * @param email     email to validate
     * @param fieldName field name to include in the error message
     * @throws IllegalArgumentException if the email is not valid
     */
    public static void exigirEmailValido(String email, String fieldName) {
        if (!validarEmail(email)) {
            throw new IllegalArgumentException(fieldName + " is invalid.");
        }
    }

    // -------------------------------------------------------------------------
    // Private helper methods
    // -------------------------------------------------------------------------

    /**
     * Checks whether all characters of a string are identical.
     * Used to reject CPFs/CNPJs such as {@code "11111111111"} that would pass the
     * check-digit calculation but are officially invalid according to the Brazilian
     * Federal Revenue Service.
     *
     * @param valor string to check
     * @return {@code true} if all characters are equal
     */
    private static boolean todosDigitosIguais(String valor) {
        char primeiro = valor.charAt(0);
        for (int i = 1; i < valor.length(); i++) {
            if (valor.charAt(i) != primeiro) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates a CPF check digit using the modulo-11 algorithm.
     *
     * <p>Multiplies each digit of {@code base} by the corresponding weight (decrementing
     * from {@code pesoInicial}), sums the products and applies modulo 11.
     * If the remainder is less than 2 the digit is 0; otherwise it is {@code 11 - remainder}.</p>
     *
     * @param base         string with the digits used in the calculation (9 or 10 characters)
     * @param pesoInicial  weight of the first digit (10 for the 1st check digit, 11 for the 2nd)
     * @return calculated check digit (0 to 9)
     */
    private static int computeCpfCheckDigit(String base, int pesoInicial) {
        int soma = 0;
        int peso = pesoInicial;

        for (int i = 0; i < base.length(); i++) {
            soma += Character.getNumericValue(base.charAt(i)) * peso;
            peso--;
        }

        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /**
     * Calculates a CNPJ check digit using the modulo-11 algorithm with variable weights.
     *
     * @param base   string with the digits used in the calculation (12 or 13 characters)
     * @param pesos  array of weights to apply sequentially to each digit of {@code base}
     * @return calculated check digit (0 to 9)
     */
    private static int computeCnpjCheckDigit(String base, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += Character.getNumericValue(base.charAt(i)) * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}

package br.com.munif.comum.utils.validacoes;

import java.util.regex.Pattern;

/**
 * Utilitários de validação e formatação para dados típicos do contexto brasileiro.
 *
 * <p>Todos os métodos são estáticos e sem estado — esta classe não deve ser instanciada.
 * Caso tente instanciá-la, será lançada uma {@link UnsupportedOperationException}.</p>
 *
 * <h2>Uso</h2>
 * <pre>{@code
 * // Validar CPF recebido do usuário
 * if (!ValidacoesBR.validarCPF(cpfDoFormulario)) {
 *     throw new IllegalArgumentException("CPF inválido.");
 * }
 *
 * // Normalizar texto antes de persistir
 * String nomeLimpo = ValidacoesBR.trimToNull(nomeDoFormulario);
 * }</pre>
 *
 * <h2>Convenções de normalização</h2>
 * <ul>
 *   <li>CPF e CNPJ são armazenados apenas com dígitos (sem pontos, traços ou barras).</li>
 *   <li>Telefones são armazenados apenas com dígitos (sem parênteses, espaços ou traços).</li>
 *   <li>CEP é armazenado apenas com dígitos (sem traço).</li>
 *   <li>E-mail é armazenado em letras minúsculas.</li>
 * </ul>
 */
public final class ValidacoesBR {

    /** Expressão regular para validação básica de e-mail. */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    /** Padrão para telefone celular brasileiro: 11 dígitos (DDD + 9 + número). */
    private static final Pattern TELEFONE_CELULAR_PATTERN =
            Pattern.compile("^\\d{11}$");

    /** Padrão para telefone fixo brasileiro: 10 dígitos (DDD + número). */
    private static final Pattern TELEFONE_FIXO_PATTERN =
            Pattern.compile("^\\d{10}$");

    /** Padrão para CEP brasileiro: exatamente 8 dígitos. */
    private static final Pattern CEP_PATTERN =
            Pattern.compile("^\\d{8}$");

    private ValidacoesBR() {
        throw new UnsupportedOperationException("Classe utilitária não pode ser instanciada.");
    }

    // -------------------------------------------------------------------------
    // Normalização de strings
    // -------------------------------------------------------------------------

    /**
     * Remove todos os caracteres não numéricos de uma string.
     *
     * <p>Útil para normalizar CPF, CNPJ, telefone e CEP antes de validar ou persistir.</p>
     *
     * @param valor string de entrada (pode conter formatação como pontos, traços etc.)
     * @return string contendo apenas dígitos, ou {@code null} se a entrada for {@code null}
     */
    public static String somenteDigitos(String valor) {
        if (valor == null) {
            return null;
        }
        return valor.replaceAll("\\D", "");
    }

    /**
     * Remove espaços em branco das bordas de uma string e retorna {@code null} se o resultado
     * ficar vazio.
     *
     * <p>Muito utilizado para normalizar campos de texto antes de persistir, evitando
     * strings vazias no banco de dados.</p>
     *
     * @param valor string de entrada
     * @return string sem espaços nas bordas, ou {@code null} se for {@code null} ou somente espaços
     */
    public static String trimToNull(String valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim();
        return texto.isBlank() ? null : texto;
    }

    /**
     * Retorna {@code true} se a string for {@code null} ou contiver apenas espaços em branco.
     *
     * @param valor string a verificar
     * @return {@code true} se vazia ou nula; {@code false} caso contrário
     */
    public static boolean isBlank(String valor) {
        return trimToNull(valor) == null;
    }

    /**
     * Retorna {@code true} se a string contiver ao menos um caractere não espaço.
     *
     * @param valor string a verificar
     * @return {@code true} se tiver conteúdo; {@code false} se for nula ou em branco
     */
    public static boolean isNotBlank(String valor) {
        return !isBlank(valor);
    }

    // -------------------------------------------------------------------------
    // Validação de CPF e CNPJ
    // -------------------------------------------------------------------------

    /**
     * Valida um CPF usando o algoritmo oficial dos dígitos verificadores.
     *
     * <p>A entrada pode estar formatada (ex.: {@code "123.456.789-09"}) ou sem formatação
     * (ex.: {@code "12345678909"}). A formatação é removida automaticamente antes da validação.</p>
     *
     * <p>CPFs com todos os dígitos iguais (ex.: {@code "111.111.111-11"}) são rejeitados,
     * pois matematicamente passariam no cálculo de dígito verificador mas são inválidos.</p>
     *
     * @param cpf CPF a validar, com ou sem formatação
     * @return {@code true} se o CPF for matematicamente válido; {@code false} caso contrário
     */
    public static boolean validarCPF(String cpf) {
        String valor = somenteDigitos(cpf);

        if (valor == null || valor.length() != 11) {
            return false;
        }
        if (todosDigitosIguais(valor)) {
            return false;
        }

        int dv1 = calcularDigitoCPF(valor.substring(0, 9), 10);
        int dv2 = calcularDigitoCPF(valor.substring(0, 9) + dv1, 11);

        return valor.equals(valor.substring(0, 9) + dv1 + dv2);
    }

    /**
     * Valida um CNPJ usando o algoritmo oficial dos dígitos verificadores.
     *
     * <p>A entrada pode estar formatada (ex.: {@code "12.345.678/0001-95"}) ou sem formatação.
     * CNPJs com todos os dígitos iguais são rejeitados.</p>
     *
     * @param cnpj CNPJ a validar, com ou sem formatação
     * @return {@code true} se o CNPJ for matematicamente válido; {@code false} caso contrário
     */
    public static boolean validarCNPJ(String cnpj) {
        String valor = somenteDigitos(cnpj);

        if (valor == null || valor.length() != 14) {
            return false;
        }
        if (todosDigitosIguais(valor)) {
            return false;
        }

        int dv1 = calcularDigitoCNPJ(valor.substring(0, 12), new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        int dv2 = calcularDigitoCNPJ(valor.substring(0, 12) + dv1, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});

        return valor.equals(valor.substring(0, 12) + dv1 + dv2);
    }

    // -------------------------------------------------------------------------
    // Validação de telefone
    // -------------------------------------------------------------------------

    /**
     * Valida um número de telefone brasileiro (celular ou fixo).
     *
     * <p>Requisitos:</p>
     * <ul>
     *   <li>10 dígitos para fixo (DDD + 8 dígitos) ou 11 dígitos para celular (DDD + 9 + 8 dígitos).</li>
     *   <li>DDD deve ser um código de área válido definido pela Anatel.</li>
     *   <li>Celular deve começar com o dígito 9 após o DDD.</li>
     * </ul>
     *
     * @param telefone número a validar, com ou sem formatação
     * @return {@code true} se for um telefone brasileiro válido; {@code false} caso contrário
     */
    public static boolean validarTelefoneBR(String telefone) {
        String valor = somenteDigitos(telefone);

        if (valor == null) {
            return false;
        }
        if (!TELEFONE_FIXO_PATTERN.matcher(valor).matches()
                && !TELEFONE_CELULAR_PATTERN.matcher(valor).matches()) {
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
     * Valida exclusivamente um número de celular brasileiro (11 dígitos, iniciando com 9 após o DDD).
     *
     * @param telefone número a validar, com ou sem formatação
     * @return {@code true} se for um celular válido; {@code false} caso contrário
     */
    public static boolean validarCelularBR(String telefone) {
        String valor = somenteDigitos(telefone);

        if (valor == null || !TELEFONE_CELULAR_PATTERN.matcher(valor).matches()) {
            return false;
        }
        return validarDDD(valor.substring(0, 2)) && valor.charAt(2) == '9';
    }

    /**
     * Valida exclusivamente um número de telefone fixo brasileiro (10 dígitos).
     *
     * @param telefone número a validar, com ou sem formatação
     * @return {@code true} se for um fixo válido; {@code false} caso contrário
     */
    public static boolean validarTelefoneFixoBR(String telefone) {
        String valor = somenteDigitos(telefone);

        if (valor == null || !TELEFONE_FIXO_PATTERN.matcher(valor).matches()) {
            return false;
        }
        return validarDDD(valor.substring(0, 2));
    }

    // -------------------------------------------------------------------------
    // Validação de CEP e e-mail
    // -------------------------------------------------------------------------

    /**
     * Valida um CEP brasileiro, que deve conter exatamente 8 dígitos.
     *
     * <p>A entrada pode estar formatada com traço (ex.: {@code "01310-100"}) ou sem formatação.</p>
     *
     * @param cep CEP a validar
     * @return {@code true} se tiver exatamente 8 dígitos; {@code false} caso contrário
     */
    public static boolean validarCEP(String cep) {
        String valor = somenteDigitos(cep);
        return valor != null && CEP_PATTERN.matcher(valor).matches();
    }

    /**
     * Valida um endereço de e-mail usando expressão regular básica.
     *
     * @param email e-mail a validar
     * @return {@code true} se o formato for válido; {@code false} caso contrário
     */
    public static boolean validarEmail(String email) {
        String valor = trimToNull(email);
        return valor != null && EMAIL_PATTERN.matcher(valor).matches();
    }

    // -------------------------------------------------------------------------
    // Validação de DDD
    // -------------------------------------------------------------------------

    /**
     * Verifica se um DDD é um código de área válido conforme a tabela da Anatel.
     *
     * @param ddd dois dígitos do DDD, com ou sem formatação
     * @return {@code true} se for um DDD válido; {@code false} caso contrário
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
    // Formatação
    // -------------------------------------------------------------------------

    /**
     * Formata um CPF no padrão {@code NNN.NNN.NNN-DD}.
     *
     * @param cpf CPF com exatamente 11 dígitos (com ou sem formatação)
     * @return CPF formatado
     * @throws IllegalArgumentException se o CPF não tiver 11 dígitos após remover não-dígitos
     */
    public static String formatarCPF(String cpf) {
        String valor = somenteDigitos(cpf);

        if (valor == null || valor.length() != 11) {
            throw new IllegalArgumentException("CPF deve conter 11 dígitos.");
        }
        return valor.substring(0, 3) + "."
                + valor.substring(3, 6) + "."
                + valor.substring(6, 9) + "-"
                + valor.substring(9, 11);
    }

    /**
     * Formata um CNPJ no padrão {@code NN.NNN.NNN/NNNN-DD}.
     *
     * @param cnpj CNPJ com exatamente 14 dígitos (com ou sem formatação)
     * @return CNPJ formatado
     * @throws IllegalArgumentException se o CNPJ não tiver 14 dígitos após remover não-dígitos
     */
    public static String formatarCNPJ(String cnpj) {
        String valor = somenteDigitos(cnpj);

        if (valor == null || valor.length() != 14) {
            throw new IllegalArgumentException("CNPJ deve conter 14 dígitos.");
        }
        return valor.substring(0, 2) + "."
                + valor.substring(2, 5) + "."
                + valor.substring(5, 8) + "/"
                + valor.substring(8, 12) + "-"
                + valor.substring(12, 14);
    }

    /**
     * Formata um CEP no padrão {@code NNNNN-NNN}.
     *
     * @param cep CEP com exatamente 8 dígitos (com ou sem formatação)
     * @return CEP formatado
     * @throws IllegalArgumentException se o CEP não tiver 8 dígitos após remover não-dígitos
     */
    public static String formatarCEP(String cep) {
        String valor = somenteDigitos(cep);

        if (valor == null || valor.length() != 8) {
            throw new IllegalArgumentException("CEP deve conter 8 dígitos.");
        }
        return valor.substring(0, 5) + "-" + valor.substring(5, 8);
    }

    /**
     * Formata um número de telefone brasileiro no padrão {@code (DD) NNNNN-NNNN} (celular)
     * ou {@code (DD) NNNN-NNNN} (fixo).
     *
     * @param telefone telefone com 10 ou 11 dígitos (com ou sem formatação)
     * @return telefone formatado
     * @throws IllegalArgumentException se o telefone não tiver 10 ou 11 dígitos
     */
    public static String formatarTelefoneBR(String telefone) {
        String valor = somenteDigitos(telefone);

        if (valor == null) {
            throw new IllegalArgumentException("Telefone inválido.");
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
        throw new IllegalArgumentException("Telefone deve conter 10 ou 11 dígitos.");
    }

    // -------------------------------------------------------------------------
    // Métodos de validação com exceção ("exigir")
    // -------------------------------------------------------------------------

    /**
     * Lança {@link IllegalArgumentException} se o CPF fornecido for inválido.
     *
     * @param cpf       CPF a validar
     * @param nomeCampo nome do campo a ser incluído na mensagem de erro (ex.: {@code "CPF"})
     * @throws IllegalArgumentException se o CPF não for válido
     */
    public static void exigirCPFValido(String cpf, String nomeCampo) {
        if (!validarCPF(cpf)) {
            throw new IllegalArgumentException(nomeCampo + " inválido.");
        }
    }

    /**
     * Lança {@link IllegalArgumentException} se o CNPJ fornecido for inválido.
     *
     * @param cnpj      CNPJ a validar
     * @param nomeCampo nome do campo a ser incluído na mensagem de erro (ex.: {@code "CNPJ"})
     * @throws IllegalArgumentException se o CNPJ não for válido
     */
    public static void exigirCNPJValido(String cnpj, String nomeCampo) {
        if (!validarCNPJ(cnpj)) {
            throw new IllegalArgumentException(nomeCampo + " inválido.");
        }
    }

    /**
     * Lança {@link IllegalArgumentException} se o telefone fornecido for inválido.
     *
     * @param telefone  telefone a validar
     * @param nomeCampo nome do campo a ser incluído na mensagem de erro
     * @throws IllegalArgumentException se o telefone não for válido
     */
    public static void exigirTelefoneValido(String telefone, String nomeCampo) {
        if (!validarTelefoneBR(telefone)) {
            throw new IllegalArgumentException(nomeCampo + " inválido.");
        }
    }

    /**
     * Lança {@link IllegalArgumentException} se o CEP fornecido for inválido.
     *
     * @param cep       CEP a validar
     * @param nomeCampo nome do campo a ser incluído na mensagem de erro
     * @throws IllegalArgumentException se o CEP não for válido
     */
    public static void exigirCEPValido(String cep, String nomeCampo) {
        if (!validarCEP(cep)) {
            throw new IllegalArgumentException(nomeCampo + " inválido.");
        }
    }

    /**
     * Lança {@link IllegalArgumentException} se o e-mail fornecido for inválido.
     *
     * @param email     e-mail a validar
     * @param nomeCampo nome do campo a ser incluído na mensagem de erro
     * @throws IllegalArgumentException se o e-mail não for válido
     */
    public static void exigirEmailValido(String email, String nomeCampo) {
        if (!validarEmail(email)) {
            throw new IllegalArgumentException(nomeCampo + " inválido.");
        }
    }

    // -------------------------------------------------------------------------
    // Métodos privados auxiliares
    // -------------------------------------------------------------------------

    /**
     * Verifica se todos os caracteres de uma string são idênticos.
     * Usado para rejeitar CPFs/CNPJs como {@code "11111111111"} que passariam no cálculo
     * de dígitos verificadores, mas são reconhecidamente inválidos pela Receita Federal.
     *
     * @param valor string a verificar
     * @return {@code true} se todos os caracteres forem iguais
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
     * Calcula um dígito verificador do CPF usando o algoritmo módulo 11.
     *
     * <p>Multiplica cada dígito da {@code base} pelo peso correspondente (decrementando a
     * partir de {@code pesoInicial}), soma os produtos e aplica o módulo 11.
     * Se o resto for menor que 2, o dígito é 0; caso contrário, é {@code 11 - resto}.</p>
     *
     * @param base         string com os dígitos usados no cálculo (9 ou 10 caracteres)
     * @param pesoInicial  peso do primeiro dígito (10 para o 1º dígito verificador, 11 para o 2º)
     * @return dígito verificador calculado (0 a 9)
     */
    private static int calcularDigitoCPF(String base, int pesoInicial) {
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
     * Calcula um dígito verificador do CNPJ usando o algoritmo módulo 11 com pesos variáveis.
     *
     * @param base   string com os dígitos usados no cálculo (12 ou 13 caracteres)
     * @param pesos  array de pesos a aplicar sequencialmente a cada dígito da {@code base}
     * @return dígito verificador calculado (0 a 9)
     */
    private static int calcularDigitoCNPJ(String base, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += Character.getNumericValue(base.charAt(i)) * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}

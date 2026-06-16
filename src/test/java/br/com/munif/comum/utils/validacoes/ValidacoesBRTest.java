package br.com.munif.comum.utils.validacoes;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class ValidacoesBRTest {

    @Test
    void deveInstanciarConstrutorPrivadoEApresentarExcecaoEsperada() throws Exception {
        Constructor<ValidacoesBR> constructor = ValidacoesBR.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception =
                assertThrows(InvocationTargetException.class, constructor::newInstance);

        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
        assertEquals(
                "Utility class cannot be instantiated.",
                exception.getCause().getMessage()
        );
    }

    @Test
    void deveRetornarSomenteDigitos() {
        assertEquals("12345678909", ValidacoesBR.somenteDigitos("123.456.789-09"));
        assertEquals("44332211000199", ValidacoesBR.somenteDigitos("44.332.211/0001-99"));
        assertEquals("", ValidacoesBR.somenteDigitos("abc"));
        assertNull(ValidacoesBR.somenteDigitos(null));
    }

    @Test
    void deveExecutarTrimToNullCorretamente() {
        assertNull(ValidacoesBR.trimToNull(null));
        assertNull(ValidacoesBR.trimToNull(""));
        assertNull(ValidacoesBR.trimToNull("   "));
        assertEquals("teste", ValidacoesBR.trimToNull("  teste  "));
    }

    @Test
    void deveValidarBlankENotBlank() {
        assertTrue(ValidacoesBR.isBlank(null));
        assertTrue(ValidacoesBR.isBlank(""));
        assertTrue(ValidacoesBR.isBlank("   "));

        assertFalse(ValidacoesBR.isBlank("abc"));
        assertTrue(ValidacoesBR.isNotBlank("abc"));
        assertFalse(ValidacoesBR.isNotBlank("   "));
    }

    @Test
    void deveValidarCpfValido() {
        assertTrue(ValidacoesBR.validarCPF("529.982.247-25"));
        assertTrue(ValidacoesBR.validarCPF("52998224725"));
    }

    @Test
    void deveInvalidarCpfNuloOuTamanhoIncorreto() {
        assertFalse(ValidacoesBR.validarCPF(null));
        assertFalse(ValidacoesBR.validarCPF(""));
        assertFalse(ValidacoesBR.validarCPF("123"));
        assertFalse(ValidacoesBR.validarCPF("123456789012"));
    }

    @Test
    void deveInvalidarCpfComTodosDigitosIguais() {
        assertFalse(ValidacoesBR.validarCPF("111.111.111-11"));
        assertFalse(ValidacoesBR.validarCPF("00000000000"));
    }

    @Test
    void deveInvalidarCpfComDigitoVerificadorErrado() {
        assertFalse(ValidacoesBR.validarCPF("529.982.247-24"));
    }

    @Test
    void deveValidarCnpjValido() {
        assertTrue(ValidacoesBR.validarCNPJ("45.723.174/0001-10"));
        assertTrue(ValidacoesBR.validarCNPJ("45723174000110"));
    }

    @Test
    void deveInvalidarCnpjNuloOuTamanhoIncorreto() {
        assertFalse(ValidacoesBR.validarCNPJ(null));
        assertFalse(ValidacoesBR.validarCNPJ(""));
        assertFalse(ValidacoesBR.validarCNPJ("123"));
        assertFalse(ValidacoesBR.validarCNPJ("123456789012345"));
    }

    @Test
    void deveInvalidarCnpjComTodosDigitosIguais() {
        assertFalse(ValidacoesBR.validarCNPJ("11.111.111/1111-11"));
        assertFalse(ValidacoesBR.validarCNPJ("00000000000000"));
    }

    @Test
    void deveInvalidarCnpjComDigitoVerificadorErrado() {
        assertFalse(ValidacoesBR.validarCNPJ("45.723.174/0001-11"));
    }

    @Test
    void deveValidarTelefoneBrCelular() {
        assertTrue(ValidacoesBR.validarTelefoneBR("44999887766"));
        assertTrue(ValidacoesBR.validarTelefoneBR("(44) 99988-7766"));
    }

    @Test
    void deveValidarTelefoneBrFixo() {
        assertTrue(ValidacoesBR.validarTelefoneBR("4433221100"));
        assertTrue(ValidacoesBR.validarTelefoneBR("(44) 3322-1100"));
    }

    @Test
    void deveInvalidarTelefoneBr() {
        assertFalse(ValidacoesBR.validarTelefoneBR(null));
        assertFalse(ValidacoesBR.validarTelefoneBR("123"));
        assertFalse(ValidacoesBR.validarTelefoneBR("00999887766")); // invalid area code
        assertFalse(ValidacoesBR.validarTelefoneBR("44899887766")); // 11 digits without 9 at the start of the number
    }

    @Test
    void deveValidarCelularBr() {
        assertTrue(ValidacoesBR.validarCelularBR("44999887766"));
        assertTrue(ValidacoesBR.validarCelularBR("(44) 99988-7766"));
    }

    @Test
    void deveInvalidarCelularBr() {
        assertFalse(ValidacoesBR.validarCelularBR(null));
        assertFalse(ValidacoesBR.validarCelularBR("4433221100"));
        assertFalse(ValidacoesBR.validarCelularBR("44899887766"));
        assertFalse(ValidacoesBR.validarCelularBR("00999887766"));
    }

    @Test
    void deveValidarTelefoneFixoBr() {
        assertTrue(ValidacoesBR.validarTelefoneFixoBR("4433221100"));
        assertTrue(ValidacoesBR.validarTelefoneFixoBR("(44) 3322-1100"));
    }

    @Test
    void deveInvalidarTelefoneFixoBr() {
        assertFalse(ValidacoesBR.validarTelefoneFixoBR(null));
        assertFalse(ValidacoesBR.validarTelefoneFixoBR("44999887766"));
        assertFalse(ValidacoesBR.validarTelefoneFixoBR("0033221100"));
    }

    @Test
    void deveValidarCep() {
        assertTrue(ValidacoesBR.validarCEP("87020-025"));
        assertTrue(ValidacoesBR.validarCEP("87020025"));
    }

    @Test
    void deveInvalidarCep() {
        assertFalse(ValidacoesBR.validarCEP(null));
        assertFalse(ValidacoesBR.validarCEP("123"));
        assertFalse(ValidacoesBR.validarCEP("87020-02"));
    }

    @Test
    void deveValidarEmail() {
        assertTrue(ValidacoesBR.validarEmail("teste@dominio.com"));
        assertTrue(ValidacoesBR.validarEmail("TESTE@DOMINIO.COM"));
        assertTrue(ValidacoesBR.validarEmail("abc.def+tag@empresa.com.br"));
    }

    @Test
    void deveInvalidarEmail() {
        assertFalse(ValidacoesBR.validarEmail(null));
        assertFalse(ValidacoesBR.validarEmail(""));
        assertFalse(ValidacoesBR.validarEmail("   "));
        assertFalse(ValidacoesBR.validarEmail("teste"));
        assertFalse(ValidacoesBR.validarEmail("teste@"));
        assertFalse(ValidacoesBR.validarEmail("@dominio.com"));
    }

    @Test
    void deveValidarDdd() {
        assertTrue(ValidacoesBR.validarDDD("11"));
        assertTrue(ValidacoesBR.validarDDD("44"));
        assertTrue(ValidacoesBR.validarDDD("98"));
    }

    @Test
    void deveInvalidarDdd() {
        assertFalse(ValidacoesBR.validarDDD(null));
        assertFalse(ValidacoesBR.validarDDD(""));
        assertFalse(ValidacoesBR.validarDDD("1"));
        assertFalse(ValidacoesBR.validarDDD("00"));
        assertFalse(ValidacoesBR.validarDDD("10"));
    }

    @Test
    void deveFormatarCpf() {
        assertEquals("529.982.247-25", ValidacoesBR.formatarCPF("52998224725"));
        assertEquals("529.982.247-25", ValidacoesBR.formatarCPF("529.982.247-25"));
    }

    @Test
    void deveLancarExcecaoAoFormatarCpfInvalido() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> ValidacoesBR.formatarCPF("123"));

        assertEquals("CPF must contain 11 digits.", exception.getMessage());
    }

    @Test
    void deveFormatarCnpj() {
        assertEquals("45.723.174/0001-10", ValidacoesBR.formatarCNPJ("45723174000110"));
        assertEquals("45.723.174/0001-10", ValidacoesBR.formatarCNPJ("45.723.174/0001-10"));
    }

    @Test
    void deveLancarExcecaoAoFormatarCnpjInvalido() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> ValidacoesBR.formatarCNPJ("123"));

        assertEquals("CNPJ must contain 14 digits.", exception.getMessage());
    }

    @Test
    void deveFormatarCep() {
        assertEquals("87020-025", ValidacoesBR.formatarCEP("87020025"));
        assertEquals("87020-025", ValidacoesBR.formatarCEP("87020-025"));
    }

    @Test
    void deveLancarExcecaoAoFormatarCepInvalido() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> ValidacoesBR.formatarCEP("123"));

        assertEquals("ZIP code must contain 8 digits.", exception.getMessage());
    }

    @Test
    void deveFormatarTelefoneCelular() {
        assertEquals("(44) 99988-7766", ValidacoesBR.formatarTelefoneBR("44999887766"));
        assertEquals("(44) 99988-7766", ValidacoesBR.formatarTelefoneBR("(44) 99988-7766"));
    }

    @Test
    void deveFormatarTelefoneFixo() {
        assertEquals("(44) 3322-1100", ValidacoesBR.formatarTelefoneBR("4433221100"));
        assertEquals("(44) 3322-1100", ValidacoesBR.formatarTelefoneBR("(44) 3322-1100"));
    }

    @Test
    void deveLancarExcecaoAoFormatarTelefoneNulo() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> ValidacoesBR.formatarTelefoneBR(null));

        assertEquals("Invalid phone number.", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoAoFormatarTelefoneComTamanhoInvalido() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> ValidacoesBR.formatarTelefoneBR("123"));

        assertEquals("Phone number must contain 10 or 11 digits.", exception.getMessage());
    }

    @Test
    void deveExigirCpfValido() {
        assertDoesNotThrow(() -> ValidacoesBR.exigirCPFValido("529.982.247-25", "CPF"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> ValidacoesBR.exigirCPFValido("11111111111", "CPF"));

        assertEquals("CPF is invalid.", exception.getMessage());
    }

    @Test
    void deveExigirCnpjValido() {
        assertDoesNotThrow(() -> ValidacoesBR.exigirCNPJValido("45.723.174/0001-10", "CNPJ"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> ValidacoesBR.exigirCNPJValido("11111111111111", "CNPJ"));

        assertEquals("CNPJ is invalid.", exception.getMessage());
    }

    @Test
    void deveExigirTelefoneValido() {
        assertDoesNotThrow(() -> ValidacoesBR.exigirTelefoneValido("(44) 99988-7766", "Telefone"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> ValidacoesBR.exigirTelefoneValido("123", "Telefone"));

        assertEquals("Telefone is invalid.", exception.getMessage());
    }

    @Test
    void deveExigirCepValido() {
        assertDoesNotThrow(() -> ValidacoesBR.exigirCEPValido("87020-025", "CEP"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> ValidacoesBR.exigirCEPValido("123", "CEP"));

        assertEquals("CEP is invalid.", exception.getMessage());
    }

    @Test
    void deveExigirEmailValido() {
        assertDoesNotThrow(() -> ValidacoesBR.exigirEmailValido("teste@dominio.com", "E-mail"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> ValidacoesBR.exigirEmailValido("teste", "E-mail"));

        assertEquals("E-mail is invalid.", exception.getMessage());
    }
}
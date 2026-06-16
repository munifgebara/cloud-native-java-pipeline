package br.com.munif.common.utils.validacoes;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class BrValidationsTest {

    @Test
    void deveInstanciarConstrutorPrivadoEApresentarExcecaoEsperada() throws Exception {
        Constructor<BrValidations> constructor = BrValidations.class.getDeclaredConstructor();
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
        assertEquals("12345678909", BrValidations.somenteDigitos("123.456.789-09"));
        assertEquals("44332211000199", BrValidations.somenteDigitos("44.332.211/0001-99"));
        assertEquals("", BrValidations.somenteDigitos("abc"));
        assertNull(BrValidations.somenteDigitos(null));
    }

    @Test
    void deveExecutarTrimToNullCorretamente() {
        assertNull(BrValidations.trimToNull(null));
        assertNull(BrValidations.trimToNull(""));
        assertNull(BrValidations.trimToNull("   "));
        assertEquals("teste", BrValidations.trimToNull("  teste  "));
    }

    @Test
    void deveValidarBlankENotBlank() {
        assertTrue(BrValidations.isBlank(null));
        assertTrue(BrValidations.isBlank(""));
        assertTrue(BrValidations.isBlank("   "));

        assertFalse(BrValidations.isBlank("abc"));
        assertTrue(BrValidations.isNotBlank("abc"));
        assertFalse(BrValidations.isNotBlank("   "));
    }

    @Test
    void deveValidarCpfValido() {
        assertTrue(BrValidations.validarCPF("529.982.247-25"));
        assertTrue(BrValidations.validarCPF("52998224725"));
    }

    @Test
    void deveInvalidarCpfNuloOuTamanhoIncorreto() {
        assertFalse(BrValidations.validarCPF(null));
        assertFalse(BrValidations.validarCPF(""));
        assertFalse(BrValidations.validarCPF("123"));
        assertFalse(BrValidations.validarCPF("123456789012"));
    }

    @Test
    void deveInvalidarCpfComTodosDigitosIguais() {
        assertFalse(BrValidations.validarCPF("111.111.111-11"));
        assertFalse(BrValidations.validarCPF("00000000000"));
    }

    @Test
    void deveInvalidarCpfComDigitoVerificadorErrado() {
        assertFalse(BrValidations.validarCPF("529.982.247-24"));
    }

    @Test
    void deveValidarCnpjValido() {
        assertTrue(BrValidations.validarCNPJ("45.723.174/0001-10"));
        assertTrue(BrValidations.validarCNPJ("45723174000110"));
    }

    @Test
    void deveInvalidarCnpjNuloOuTamanhoIncorreto() {
        assertFalse(BrValidations.validarCNPJ(null));
        assertFalse(BrValidations.validarCNPJ(""));
        assertFalse(BrValidations.validarCNPJ("123"));
        assertFalse(BrValidations.validarCNPJ("123456789012345"));
    }

    @Test
    void deveInvalidarCnpjComTodosDigitosIguais() {
        assertFalse(BrValidations.validarCNPJ("11.111.111/1111-11"));
        assertFalse(BrValidations.validarCNPJ("00000000000000"));
    }

    @Test
    void deveInvalidarCnpjComDigitoVerificadorErrado() {
        assertFalse(BrValidations.validarCNPJ("45.723.174/0001-11"));
    }

    @Test
    void deveValidarTelefoneBrCelular() {
        assertTrue(BrValidations.validarTelefoneBR("44999887766"));
        assertTrue(BrValidations.validarTelefoneBR("(44) 99988-7766"));
    }

    @Test
    void deveValidarTelefoneBrFixo() {
        assertTrue(BrValidations.validarTelefoneBR("4433221100"));
        assertTrue(BrValidations.validarTelefoneBR("(44) 3322-1100"));
    }

    @Test
    void deveInvalidarTelefoneBr() {
        assertFalse(BrValidations.validarTelefoneBR(null));
        assertFalse(BrValidations.validarTelefoneBR("123"));
        assertFalse(BrValidations.validarTelefoneBR("00999887766")); // invalid area code
        assertFalse(BrValidations.validarTelefoneBR("44899887766")); // 11 digits without 9 at the start of the number
    }

    @Test
    void deveValidarCelularBr() {
        assertTrue(BrValidations.validarCelularBR("44999887766"));
        assertTrue(BrValidations.validarCelularBR("(44) 99988-7766"));
    }

    @Test
    void deveInvalidarCelularBr() {
        assertFalse(BrValidations.validarCelularBR(null));
        assertFalse(BrValidations.validarCelularBR("4433221100"));
        assertFalse(BrValidations.validarCelularBR("44899887766"));
        assertFalse(BrValidations.validarCelularBR("00999887766"));
    }

    @Test
    void deveValidarTelefoneFixoBr() {
        assertTrue(BrValidations.validarTelefoneFixoBR("4433221100"));
        assertTrue(BrValidations.validarTelefoneFixoBR("(44) 3322-1100"));
    }

    @Test
    void deveInvalidarTelefoneFixoBr() {
        assertFalse(BrValidations.validarTelefoneFixoBR(null));
        assertFalse(BrValidations.validarTelefoneFixoBR("44999887766"));
        assertFalse(BrValidations.validarTelefoneFixoBR("0033221100"));
    }

    @Test
    void deveValidarCep() {
        assertTrue(BrValidations.validarCEP("87020-025"));
        assertTrue(BrValidations.validarCEP("87020025"));
    }

    @Test
    void deveInvalidarCep() {
        assertFalse(BrValidations.validarCEP(null));
        assertFalse(BrValidations.validarCEP("123"));
        assertFalse(BrValidations.validarCEP("87020-02"));
    }

    @Test
    void deveValidarEmail() {
        assertTrue(BrValidations.validarEmail("teste@dominio.com"));
        assertTrue(BrValidations.validarEmail("TESTE@DOMINIO.COM"));
        assertTrue(BrValidations.validarEmail("abc.def+tag@empresa.com.br"));
    }

    @Test
    void deveInvalidarEmail() {
        assertFalse(BrValidations.validarEmail(null));
        assertFalse(BrValidations.validarEmail(""));
        assertFalse(BrValidations.validarEmail("   "));
        assertFalse(BrValidations.validarEmail("teste"));
        assertFalse(BrValidations.validarEmail("teste@"));
        assertFalse(BrValidations.validarEmail("@dominio.com"));
    }

    @Test
    void deveValidarDdd() {
        assertTrue(BrValidations.validarDDD("11"));
        assertTrue(BrValidations.validarDDD("44"));
        assertTrue(BrValidations.validarDDD("98"));
    }

    @Test
    void deveInvalidarDdd() {
        assertFalse(BrValidations.validarDDD(null));
        assertFalse(BrValidations.validarDDD(""));
        assertFalse(BrValidations.validarDDD("1"));
        assertFalse(BrValidations.validarDDD("00"));
        assertFalse(BrValidations.validarDDD("10"));
    }

    @Test
    void deveFormatarCpf() {
        assertEquals("529.982.247-25", BrValidations.formatarCPF("52998224725"));
        assertEquals("529.982.247-25", BrValidations.formatarCPF("529.982.247-25"));
    }

    @Test
    void deveLancarExcecaoAoFormatarCpfInvalido() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> BrValidations.formatarCPF("123"));

        assertEquals("CPF must contain 11 digits.", exception.getMessage());
    }

    @Test
    void deveFormatarCnpj() {
        assertEquals("45.723.174/0001-10", BrValidations.formatarCNPJ("45723174000110"));
        assertEquals("45.723.174/0001-10", BrValidations.formatarCNPJ("45.723.174/0001-10"));
    }

    @Test
    void deveLancarExcecaoAoFormatarCnpjInvalido() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> BrValidations.formatarCNPJ("123"));

        assertEquals("CNPJ must contain 14 digits.", exception.getMessage());
    }

    @Test
    void deveFormatarCep() {
        assertEquals("87020-025", BrValidations.formatarCEP("87020025"));
        assertEquals("87020-025", BrValidations.formatarCEP("87020-025"));
    }

    @Test
    void deveLancarExcecaoAoFormatarCepInvalido() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> BrValidations.formatarCEP("123"));

        assertEquals("ZIP code must contain 8 digits.", exception.getMessage());
    }

    @Test
    void deveFormatarTelefoneCelular() {
        assertEquals("(44) 99988-7766", BrValidations.formatarTelefoneBR("44999887766"));
        assertEquals("(44) 99988-7766", BrValidations.formatarTelefoneBR("(44) 99988-7766"));
    }

    @Test
    void deveFormatarTelefoneFixo() {
        assertEquals("(44) 3322-1100", BrValidations.formatarTelefoneBR("4433221100"));
        assertEquals("(44) 3322-1100", BrValidations.formatarTelefoneBR("(44) 3322-1100"));
    }

    @Test
    void deveLancarExcecaoAoFormatarTelefoneNulo() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> BrValidations.formatarTelefoneBR(null));

        assertEquals("Invalid phone number.", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoAoFormatarTelefoneComTamanhoInvalido() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> BrValidations.formatarTelefoneBR("123"));

        assertEquals("Phone number must contain 10 or 11 digits.", exception.getMessage());
    }

    @Test
    void deveExigirCpfValido() {
        assertDoesNotThrow(() -> BrValidations.exigirCPFValido("529.982.247-25", "CPF"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> BrValidations.exigirCPFValido("11111111111", "CPF"));

        assertEquals("CPF is invalid.", exception.getMessage());
    }

    @Test
    void deveExigirCnpjValido() {
        assertDoesNotThrow(() -> BrValidations.exigirCNPJValido("45.723.174/0001-10", "CNPJ"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> BrValidations.exigirCNPJValido("11111111111111", "CNPJ"));

        assertEquals("CNPJ is invalid.", exception.getMessage());
    }

    @Test
    void deveExigirTelefoneValido() {
        assertDoesNotThrow(() -> BrValidations.exigirTelefoneValido("(44) 99988-7766", "Telefone"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> BrValidations.exigirTelefoneValido("123", "Telefone"));

        assertEquals("Telefone is invalid.", exception.getMessage());
    }

    @Test
    void deveExigirCepValido() {
        assertDoesNotThrow(() -> BrValidations.exigirCEPValido("87020-025", "CEP"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> BrValidations.exigirCEPValido("123", "CEP"));

        assertEquals("CEP is invalid.", exception.getMessage());
    }

    @Test
    void deveExigirEmailValido() {
        assertDoesNotThrow(() -> BrValidations.exigirEmailValido("teste@dominio.com", "And-mail"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> BrValidations.exigirEmailValido("teste", "And-mail"));

        assertEquals("And-mail is invalid.", exception.getMessage());
    }
}
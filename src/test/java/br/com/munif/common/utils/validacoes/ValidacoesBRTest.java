package br.com.munif.common.utils.validacoes;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class BrValidationsTest {

    @Test
    void shouldInstantiateConstructorPrivateAndPresentExceptionExpected() throws Exception {
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
    void shouldReturnOnlyDigits() {
        assertEquals("12345678909", BrValidations.somenteDigitos("123.456.789-09"));
        assertEquals("44332211000199", BrValidations.somenteDigitos("44.332.211/0001-99"));
        assertEquals("", BrValidations.somenteDigitos("abc"));
        assertNull(BrValidations.somenteDigitos(null));
    }

    @Test
    void shouldExecuteTrimToNullCorrectly() {
        assertNull(BrValidations.trimToNull(null));
        assertNull(BrValidations.trimToNull(""));
        assertNull(BrValidations.trimToNull("   "));
        assertEquals("teste", BrValidations.trimToNull("  teste  "));
    }

    @Test
    void shouldValidateBlankAndNotBlank() {
        assertTrue(BrValidations.isBlank(null));
        assertTrue(BrValidations.isBlank(""));
        assertTrue(BrValidations.isBlank("   "));

        assertFalse(BrValidations.isBlank("abc"));
        assertTrue(BrValidations.isNotBlank("abc"));
        assertFalse(BrValidations.isNotBlank("   "));
    }

    @Test
    void shouldValidateCpfValid() {
        assertTrue(BrValidations.validarCPF("529.982.247-25"));
        assertTrue(BrValidations.validarCPF("52998224725"));
    }

    @Test
    void shouldInvalidateCpfNullOrSizeIncorrect() {
        assertFalse(BrValidations.validarCPF(null));
        assertFalse(BrValidations.validarCPF(""));
        assertFalse(BrValidations.validarCPF("123"));
        assertFalse(BrValidations.validarCPF("123456789012"));
    }

    @Test
    void shouldInvalidateCpfWithAllDigitsEqual() {
        assertFalse(BrValidations.validarCPF("111.111.111-11"));
        assertFalse(BrValidations.validarCPF("00000000000"));
    }

    @Test
    void shouldInvalidateCpfWithDigitCheckerWrong() {
        assertFalse(BrValidations.validarCPF("529.982.247-24"));
    }

    @Test
    void shouldValidateCnpjValid() {
        assertTrue(BrValidations.validarCNPJ("45.723.174/0001-10"));
        assertTrue(BrValidations.validarCNPJ("45723174000110"));
    }

    @Test
    void shouldInvalidateCnpjNullOrSizeIncorrect() {
        assertFalse(BrValidations.validarCNPJ(null));
        assertFalse(BrValidations.validarCNPJ(""));
        assertFalse(BrValidations.validarCNPJ("123"));
        assertFalse(BrValidations.validarCNPJ("123456789012345"));
    }

    @Test
    void shouldInvalidateCnpjWithAllDigitsEqual() {
        assertFalse(BrValidations.validarCNPJ("11.111.111/1111-11"));
        assertFalse(BrValidations.validarCNPJ("00000000000000"));
    }

    @Test
    void shouldInvalidateCnpjWithDigitCheckerWrong() {
        assertFalse(BrValidations.validarCNPJ("45.723.174/0001-11"));
    }

    @Test
    void shouldValidatePhoneBrMobile() {
        assertTrue(BrValidations.validateBrazilianPhone("44999887766"));
        assertTrue(BrValidations.validateBrazilianPhone("(44) 99988-7766"));
    }

    @Test
    void shouldValidatePhoneBrLandline() {
        assertTrue(BrValidations.validateBrazilianPhone("4433221100"));
        assertTrue(BrValidations.validateBrazilianPhone("(44) 3322-1100"));
    }

    @Test
    void shouldInvalidatePhoneBr() {
        assertFalse(BrValidations.validateBrazilianPhone(null));
        assertFalse(BrValidations.validateBrazilianPhone("123"));
        assertFalse(BrValidations.validateBrazilianPhone("00999887766")); // invalid area code
        assertFalse(BrValidations.validateBrazilianPhone("44899887766")); // 11 digits without 9 at the start of the number
    }

    @Test
    void shouldValidateMobileBr() {
        assertTrue(BrValidations.validarCelularBR("44999887766"));
        assertTrue(BrValidations.validarCelularBR("(44) 99988-7766"));
    }

    @Test
    void shouldInvalidateMobileBr() {
        assertFalse(BrValidations.validarCelularBR(null));
        assertFalse(BrValidations.validarCelularBR("4433221100"));
        assertFalse(BrValidations.validarCelularBR("44899887766"));
        assertFalse(BrValidations.validarCelularBR("00999887766"));
    }

    @Test
    void shouldValidatePhoneLandlineBr() {
        assertTrue(BrValidations.validateBrazilianLandline("4433221100"));
        assertTrue(BrValidations.validateBrazilianLandline("(44) 3322-1100"));
    }

    @Test
    void shouldInvalidatePhoneLandlineBr() {
        assertFalse(BrValidations.validateBrazilianLandline(null));
        assertFalse(BrValidations.validateBrazilianLandline("44999887766"));
        assertFalse(BrValidations.validateBrazilianLandline("0033221100"));
    }

    @Test
    void shouldValidateCep() {
        assertTrue(BrValidations.validarCEP("87020-025"));
        assertTrue(BrValidations.validarCEP("87020025"));
    }

    @Test
    void shouldInvalidateCep() {
        assertFalse(BrValidations.validarCEP(null));
        assertFalse(BrValidations.validarCEP("123"));
        assertFalse(BrValidations.validarCEP("87020-02"));
    }

    @Test
    void shouldValidateEmail() {
        assertTrue(BrValidations.validarEmail("teste@dominio.com"));
        assertTrue(BrValidations.validarEmail("TESTE@DOMINIO.COM"));
        assertTrue(BrValidations.validarEmail("abc.def+tag@empresa.com.br"));
    }

    @Test
    void shouldInvalidateEmail() {
        assertFalse(BrValidations.validarEmail(null));
        assertFalse(BrValidations.validarEmail(""));
        assertFalse(BrValidations.validarEmail("   "));
        assertFalse(BrValidations.validarEmail("teste"));
        assertFalse(BrValidations.validarEmail("teste@"));
        assertFalse(BrValidations.validarEmail("@dominio.com"));
    }

    @Test
    void shouldValidateAreaCode() {
        assertTrue(BrValidations.validarDDD("11"));
        assertTrue(BrValidations.validarDDD("44"));
        assertTrue(BrValidations.validarDDD("98"));
    }

    @Test
    void shouldInvalidateAreaCode() {
        assertFalse(BrValidations.validarDDD(null));
        assertFalse(BrValidations.validarDDD(""));
        assertFalse(BrValidations.validarDDD("1"));
        assertFalse(BrValidations.validarDDD("00"));
        assertFalse(BrValidations.validarDDD("10"));
    }

    @Test
    void shouldFormatCpf() {
        assertEquals("529.982.247-25", BrValidations.formatarCPF("52998224725"));
        assertEquals("529.982.247-25", BrValidations.formatarCPF("529.982.247-25"));
    }

    @Test
    void shouldThrowExceptionOnFormatCpfInvalid() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> BrValidations.formatarCPF("123"));

        assertEquals("CPF must contain 11 digits.", exception.getMessage());
    }

    @Test
    void shouldFormatCnpj() {
        assertEquals("45.723.174/0001-10", BrValidations.formatarCNPJ("45723174000110"));
        assertEquals("45.723.174/0001-10", BrValidations.formatarCNPJ("45.723.174/0001-10"));
    }

    @Test
    void shouldThrowExceptionOnFormatCnpjInvalid() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> BrValidations.formatarCNPJ("123"));

        assertEquals("CNPJ must contain 14 digits.", exception.getMessage());
    }

    @Test
    void shouldFormatCep() {
        assertEquals("87020-025", BrValidations.formatarCEP("87020025"));
        assertEquals("87020-025", BrValidations.formatarCEP("87020-025"));
    }

    @Test
    void shouldThrowExceptionOnFormatCepInvalid() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> BrValidations.formatarCEP("123"));

        assertEquals("ZIP code must contain 8 digits.", exception.getMessage());
    }

    @Test
    void shouldFormatPhoneMobile() {
        assertEquals("(44) 99988-7766", BrValidations.formatBrazilianPhone("44999887766"));
        assertEquals("(44) 99988-7766", BrValidations.formatBrazilianPhone("(44) 99988-7766"));
    }

    @Test
    void shouldFormatPhoneLandline() {
        assertEquals("(44) 3322-1100", BrValidations.formatBrazilianPhone("4433221100"));
        assertEquals("(44) 3322-1100", BrValidations.formatBrazilianPhone("(44) 3322-1100"));
    }

    @Test
    void shouldThrowExceptionOnFormatPhoneNull() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> BrValidations.formatBrazilianPhone(null));

        assertEquals("Invalid phone number.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionOnFormatPhoneWithSizeInvalid() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> BrValidations.formatBrazilianPhone("123"));

        assertEquals("Phone number must contain 10 or 11 digits.", exception.getMessage());
    }

    @Test
    void shouldRequireCpfValid() {
        assertDoesNotThrow(() -> BrValidations.exigirCPFValido("529.982.247-25", "CPF"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> BrValidations.exigirCPFValido("11111111111", "CPF"));

        assertEquals("CPF is invalid.", exception.getMessage());
    }

    @Test
    void shouldRequireCnpjValid() {
        assertDoesNotThrow(() -> BrValidations.exigirCNPJValido("45.723.174/0001-10", "CNPJ"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> BrValidations.exigirCNPJValido("11111111111111", "CNPJ"));

        assertEquals("CNPJ is invalid.", exception.getMessage());
    }

    @Test
    void shouldRequirePhoneValid() {
        assertDoesNotThrow(() -> BrValidations.requireValidPhone("(44) 99988-7766", "Telefone"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> BrValidations.requireValidPhone("123", "Telefone"));

        assertEquals("Telefone is invalid.", exception.getMessage());
    }

    @Test
    void shouldRequireCepValid() {
        assertDoesNotThrow(() -> BrValidations.exigirCEPValido("87020-025", "CEP"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> BrValidations.exigirCEPValido("123", "CEP"));

        assertEquals("CEP is invalid.", exception.getMessage());
    }

    @Test
    void shouldRequireEmailValid() {
        assertDoesNotThrow(() -> BrValidations.exigirEmailValido("teste@dominio.com", "And-mail"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> BrValidations.exigirEmailValido("teste", "And-mail"));

        assertEquals("And-mail is invalid.", exception.getMessage());
    }
}
package br.com.stella.api.exception;

import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

final class TestValidationSupport {

    private TestValidationSupport() {
    }

    static MethodArgumentNotValidException methodArgumentNotValid(String field, String message) {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", field, message));
        return new MethodArgumentNotValidException(methodParameter(), bindingResult);
    }

    private static MethodParameter methodParameter() {
        try {
            Method method = TestValidationSupport.class.getDeclaredMethod("sample", String.class);
            return new MethodParameter(method, 0);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @SuppressWarnings("unused")
    private static void sample(String value) {
    }
}

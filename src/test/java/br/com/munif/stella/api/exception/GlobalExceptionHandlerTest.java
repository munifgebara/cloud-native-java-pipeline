package br.com.munif.stella.api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void deveRetornarMensagemDeRegraDeNegocioComContexto() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v0/pessoas");

        var response = handler.tratarRegraNegocio(new IllegalArgumentException("CPF/CNPJ é obrigatório."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsEntry("status", 400)
                .containsEntry("erro", "CPF/CNPJ é obrigatório.")
                .containsEntry("path", "/api/v0/pessoas");
    }

    @Test
    void deveOcultarDetalheTecnicoEmErroInesperado() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v0/dashboard");

        var response = handler.tratarErroInesperado(new RuntimeException("detalhe interno"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .containsEntry("status", 500)
                .containsEntry("erro", "Erro inesperado ao processar a solicitação.")
                .containsEntry("path", "/api/v0/dashboard");
    }

    @Test
    void deveRetornarCamposEmErroDeValidacao() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v0/pessoas");
        Map<String, Object> body = handler.tratarValidacao(
                TestValidationSupport.methodArgumentNotValid("nome", "Nome é obrigatório."),
                request
        ).getBody();

        assertThat(body)
                .containsEntry("status", 400)
                .containsEntry("erro", "Dados inválidos.")
                .containsEntry("path", "/api/v0/pessoas");
        assertThat(body).extracting("campos")
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.map(String.class, String.class))
                .containsEntry("nome", "Nome é obrigatório.");
    }
}

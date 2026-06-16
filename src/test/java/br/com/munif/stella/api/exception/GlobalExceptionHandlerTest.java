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

        var response = handler.handleBusinessRule(new IllegalArgumentException("CPF/CNPJ is required."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsEntry("status", 400)
                .containsEntry("erro", "CPF/CNPJ is required.")
                .containsEntry("path", "/api/v0/pessoas");
    }

    @Test
    void deveOcultarDetalheTecnicoEmErroInesperado() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v0/dashboard");

        var response = handler.handleUnexpectedError(new RuntimeException("internal detail"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .containsEntry("status", 500)
                .containsEntry("erro", "Unexpected error while processing the request.")
                .containsEntry("path", "/api/v0/dashboard");
    }

    @Test
    void deveRetornarCamposEmErroDeValidacao() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v0/pessoas");
        Map<String, Object> body = handler.handleValidation(
                TestValidationSupport.methodArgumentNotValid("nome", "Name is required."),
                request
        ).getBody();

        assertThat(body)
                .containsEntry("status", 400)
                .containsEntry("erro", "Invalid data.")
                .containsEntry("path", "/api/v0/pessoas");
        assertThat(body).extracting("campos")
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.map(String.class, String.class))
                .containsEntry("nome", "Name is required.");
    }

    @Test
    void deveRetornarStatusDeBloqueioDeUsoIa() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v0/itens-mestre/imagem-ia");

        var response = handler.handleAiUsageLimit(new AiUsageLimitException(HttpStatus.TOO_MANY_REQUESTS, "Daily limit for OpenAI image generation reached."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody())
                .containsEntry("status", 429)
                .containsEntry("erro", "Daily limit for OpenAI image generation reached.")
                .containsEntry("path", "/api/v0/itens-mestre/imagem-ia");
    }

    @Test
    void deveRetornar502ComMensagemDaExcecaoParaFalhaDeIntegracao() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v0/itens-mestre/imagem-ia");

        var response = handler.handleExternalIntegration(
                new ExternalIntegrationException("OpenAI returned an empty response for the image."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody())
                .containsEntry("status", 502)
                .containsEntry("erro", "OpenAI returned an empty response for the image.")
                .containsEntry("path", "/api/v0/itens-mestre/imagem-ia");
    }

    @Test
    void deveRetornar500ComMensagemGenericaParaEstadoIlegal() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v0/itens-mestre");

        var response = handler.handleUnexpectedState(
                new IllegalStateException("OPENAI_API_KEY not configured in the environment."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .containsEntry("status", 500)
                .containsEntry("erro", "Unexpected error while processing the request.")
                .containsEntry("path", "/api/v0/itens-mestre");
    }
}

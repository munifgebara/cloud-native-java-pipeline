package br.com.stella.api.service;

import br.com.stella.api.config.KeycloakProperties;
import br.com.stella.api.exception.ExternalIntegrationException;
import br.com.stella.api.exception.IdentityException;
import br.com.stella.api.dto.LoginRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakLoginServiceTest {

    private MockRestServiceServer server;
    private KeycloakLoginService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new KeycloakLoginService(
                new KeycloakProperties("http://keycloak", "stella", "stella-cli", "master", "admin-cli", "admin", "admin", null),
                builder
        );
    }

    @Test
    void deveRealizarLoginComPasswordGrant() {
        server.expect(once(), requestTo("http://keycloak/realms/stella/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("client_id=stella-cli&grant_type=password&username=user&password=senha123"))
                .andRespond(withSuccess("""
                        {
                          "access_token": "access",
                          "refresh_token": "refresh",
                          "token_type": "Bearer",
                          "expires_in": 300
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = service.login(new LoginRequestDTO("user", "senha123"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(300L);
        server.verify();
    }

    @Test
    void deveRejeitarRespostaVaziaDoKeycloak() {
        server.expect(once(), requestTo("http://keycloak/realms/stella/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.login(new LoginRequestDTO("user", "senha123")))
                .isInstanceOf(ExternalIntegrationException.class)
                .hasMessage("Empty response from Keycloak.");

        server.verify();
    }

    @Test
    void deveRetornarUnauthorizedQuandoCredenciaisInvalidas() {
        server.expect(once(), requestTo("http://keycloak/realms/stella/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> service.login(new LoginRequestDTO("user", "senha-invalida")))
                .isInstanceOf(IdentityException.class)
                .hasMessage("Invalid credentials.")
                .extracting(ex -> ((IdentityException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        server.verify();
    }

    @Test
    void deveTraduzirFalhaDeConexaoComKeycloakComoServicoIndisponivel() {
        KeycloakLoginService servicoIndisponivel = new KeycloakLoginService(
                new KeycloakProperties("http://127.0.0.1:1", "stella", "stella-cli", "master", "admin-cli", "admin", "admin", null),
                RestClient.builder()
        );

        assertThatThrownBy(() -> servicoIndisponivel.login(new LoginRequestDTO("user", "senha123")))
                .isInstanceOf(ExternalIntegrationException.class)
                .hasMessage("Identity service unavailable.");
    }
}

package br.com.munif.stella.api.service;

import br.com.munif.stella.api.config.KeycloakProperties;
import br.com.munif.stella.api.exception.IntegracaoExternaException;
import br.com.munif.stella.api.dto.LoginRequestDTO;
import br.com.munif.stella.api.dto.LoginResponseDTO;
import br.com.munif.stella.api.observability.StructuredBusinessLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Service
public class KeycloakLoginService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakLoginService.class);

    private final RestClient restClient;
    private final KeycloakProperties keycloakProperties;

    public KeycloakLoginService(KeycloakProperties keycloakProperties, RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
        this.keycloakProperties = keycloakProperties;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakProperties.publicClientId());
        form.add("grant_type", "password");
        form.add("username", request.username());
        form.add("password", request.password());

        @SuppressWarnings("unchecked")
        Map<String, Object> response;
        long inicio = System.nanoTime();
        try {
            response = restClient.post()
                    .uri(keycloakProperties.tokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException ex) {
            logFailure(request.username(), inicio, ex);
            throw ex;
        } catch (ResourceAccessException ex) {
            logFailure(request.username(), inicio, ex);
            throw new IntegracaoExternaException("Serviço de identidade indisponível.", ex);
        }

        if (response == null) {
            StructuredBusinessLogger.warn(log, "security", "login-failed", StructuredBusinessLogger.fields(
                    "user_id", request.username(),
                    "identity_provider", "keycloak",
                    "duration_ms", elapsedMillis(inicio),
                    "success", false
            ));
            throw new IntegracaoExternaException("Resposta vazia do Keycloak.");
        }

        StructuredBusinessLogger.info(log, "security", "login-succeeded", StructuredBusinessLogger.fields(
                "user_id", request.username(),
                "identity_provider", "keycloak",
                "duration_ms", elapsedMillis(inicio),
                "success", true
        ));
        return new LoginResponseDTO(
                (String) response.get("access_token"),
                (String) response.get("refresh_token"),
                (String) response.get("token_type"),
                response.get("expires_in") == null ? null : ((Number) response.get("expires_in")).longValue()
        );
    }

    private void logFailure(String username, long inicio, Exception ex) {
        StructuredBusinessLogger.warn(log, "security", "login-failed", StructuredBusinessLogger.fields(
                "user_id", username,
                "identity_provider", "keycloak",
                "duration_ms", elapsedMillis(inicio),
                "success", false
        ));
    }

    private long elapsedMillis(long inicio) {
        return (System.nanoTime() - inicio) / 1_000_000L;
    }
}

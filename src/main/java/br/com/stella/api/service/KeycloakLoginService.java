package br.com.stella.api.service;

import br.com.stella.api.config.KeycloakProperties;
import br.com.stella.api.exception.ExternalIntegrationException;
import br.com.stella.api.exception.IdentityException;
import br.com.stella.api.dto.LoginRequestDTO;
import br.com.stella.api.dto.LoginResponseDTO;
import br.com.stella.api.dto.RefreshTokenRequestDTO;
import br.com.stella.api.observability.StructuredBusinessLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

        return tokenExchange(form, request.username(), "login");
    }

    public LoginResponseDTO refresh(RefreshTokenRequestDTO request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new IdentityException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token is required.",
                    new IllegalArgumentException("refreshToken is required")
            );
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakProperties.publicClientId());
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", request.refreshToken());

        return tokenExchange(form, "refresh-token", "refresh");
    }

    private LoginResponseDTO tokenExchange(MultiValueMap<String, String> form, String userId, String operation) {
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
            logFailure(userId, operation, inicio, ex);
            if (ex.getStatusCode().is4xxClientError()) {
                throw new IdentityException(HttpStatus.UNAUTHORIZED, unauthorizedMessage(operation), ex);
            }
            throw new ExternalIntegrationException("Identity service unavailable.", ex);
        } catch (ResourceAccessException ex) {
            logFailure(userId, operation, inicio, ex);
            throw new ExternalIntegrationException("Identity service unavailable.", ex);
        }

        if (response == null) {
            StructuredBusinessLogger.warn(log, "security", "login-failed", StructuredBusinessLogger.fields(
                    "user_id", userId,
                    "identity_provider", "keycloak",
                    "auth_operation", operation,
                    "duration_ms", elapsedMillis(inicio),
                    "success", false
            ));
            throw new ExternalIntegrationException("Empty response from Keycloak.");
        }

        StructuredBusinessLogger.info(log, "security", "login-succeeded", StructuredBusinessLogger.fields(
                "user_id", userId,
                "identity_provider", "keycloak",
                "auth_operation", operation,
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

    private String unauthorizedMessage(String operation) {
        return "refresh".equals(operation) ? "Refresh token expired or invalid." : "Invalid credentials.";
    }

    private void logFailure(String username, String operation, long inicio, Exception ex) {
        StructuredBusinessLogger.warn(log, "security", "login-failed", StructuredBusinessLogger.fields(
                "user_id", username,
                "identity_provider", "keycloak",
                "auth_operation", operation,
                "duration_ms", elapsedMillis(inicio),
                "success", false
        ));
    }

    private long elapsedMillis(long inicio) {
        return (System.nanoTime() - inicio) / 1_000_000L;
    }
}

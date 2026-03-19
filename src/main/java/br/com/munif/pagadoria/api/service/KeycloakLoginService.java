package br.com.munif.pagadoria.api.service;

import br.com.munif.pagadoria.api.config.KeycloakProperties;
import br.com.munif.pagadoria.api.dto.LoginRequestDTO;
import br.com.munif.pagadoria.api.dto.LoginResponseDTO;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class KeycloakLoginService {

    private final RestClient restClient;
    private final KeycloakProperties keycloakProperties;

    public KeycloakLoginService(KeycloakProperties keycloakProperties) {
        this.restClient = RestClient.builder().build();
        this.keycloakProperties = keycloakProperties;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakProperties.publicClientId());
        form.add("grant_type", "password");
        form.add("username", request.username());
        form.add("password", request.password());

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri(keycloakProperties.tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Resposta vazia do Keycloak.");
        }

        return new LoginResponseDTO(
                (String) response.get("access_token"),
                (String) response.get("refresh_token"),
                (String) response.get("token_type"),
                response.get("expires_in") == null ? null : ((Number) response.get("expires_in")).longValue()
        );
    }
}


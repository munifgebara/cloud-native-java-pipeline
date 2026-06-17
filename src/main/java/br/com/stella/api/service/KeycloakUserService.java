package br.com.stella.api.service;

import br.com.stella.api.config.KeycloakProperties;
import br.com.stella.api.dto.AlterarSenhaDTO;
import br.com.stella.api.dto.MeuPerfilResponseDTO;
import br.com.stella.api.dto.MeuPerfilUpdateDTO;
import br.com.stella.api.dto.UserCreateDTO;
import br.com.stella.api.dto.UserResponseDTO;
import br.com.stella.api.dto.UserUpdateDTO;
import br.com.stella.api.exception.ExternalIntegrationException;
import br.com.stella.api.exception.IdentityException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@Service
public class KeycloakUserService {

    private static final List<String> ROLES_GERENCIADAS = List.of("admin", "proprietario", "user");

    private final RestClient restClient;
    private final KeycloakProperties keycloakProperties;

    public KeycloakUserService(KeycloakProperties keycloakProperties, RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
        this.keycloakProperties = keycloakProperties;
    }

    public List<UserResponseDTO> listar() {
        List<Map<String, Object>> users = getList("/users?briefRepresentation=false");
        return users.stream()
                .map(user -> toUserResponse(user, listUserRoles((String) user.get("id"))))
                .toList();
    }

    public UserResponseDTO findById(String id) {
        return toUserResponse(fetchUserMap(id), listUserRoles(id));
    }

    public UserResponseDTO create(UserCreateDTO dto) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", dto.username().trim());
        payload.put("firstName", trimToNull(dto.firstName()));
        payload.put("lastName", trimToNull(dto.lastName()));
        payload.put("email", trimToNull(dto.email()));
        payload.put("enabled", dto.enabled() == null || dto.enabled());
        payload.put("emailVerified", false);
        payload.put("credentials", List.of(Map.of(
                "type", "password",
                "value", dto.password(),
                "temporary", false
        )));

        ResponseEntity<Void> response = executarKeycloak(() -> restClient.post()
                .uri(keycloakProperties.adminRealmUrl() + "/users")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity());

        String id = extrairIdCriado(response.getHeaders().getLocation());
        updateRoles(id, dto.roles());
        return findById(id);
    }

    public UserResponseDTO update(String id, UserUpdateDTO dto) {
        Map<String, Object> user = fetchUserMap(id);
        user.put("firstName", trimToNull(dto.firstName()));
        user.put("lastName", trimToNull(dto.lastName()));
        user.put("email", trimToNull(dto.email()));

        if (dto.enabled() != null) {
            user.put("enabled", dto.enabled());
        }

        put("/users/" + id, user);
        updateRoles(id, dto.roles());
        return findById(id);
    }

    public void alterarStatus(String id, boolean enabled) {
        Map<String, Object> user = fetchUserMap(id);
        user.put("enabled", enabled);
        put("/users/" + id, user);
    }

    public MeuPerfilResponseDTO meuPerfil(Jwt jwt) {
        return new MeuPerfilResponseDTO(
                jwt.getSubject(),
                firstNonBlank(
                        jwt.getClaimAsString("preferred_username"),
                        jwt.getClaimAsString("username"),
                        jwt.getSubject()
                ),
                firstNonBlank(jwt.getClaimAsString("given_name"), jwt.getClaimAsString("firstName")),
                firstNonBlank(jwt.getClaimAsString("family_name"), jwt.getClaimAsString("lastName")),
                jwt.getClaimAsString("email"),
                rolesFromJwt(jwt),
                keycloakProperties.accountUrl()
        );
    }

    public MeuPerfilResponseDTO atualizarMeuPerfil(Jwt jwt, MeuPerfilUpdateDTO dto) {
        Map<String, Object> user = fetchUserMap(jwt.getSubject());
        user.put("firstName", trimToNull(dto.firstName()));
        user.put("lastName", trimToNull(dto.lastName()));
        user.put("email", trimToNull(dto.email()));
        put("/users/" + jwt.getSubject(), user);
        return toMeuPerfil(findById(jwt.getSubject()));
    }

    public void alterarMinhaSenha(Jwt jwt, AlterarSenhaDTO dto) {
        validarSenhaAtual(jwt.getClaimAsString("preferred_username"), dto.senhaAtual());
        redefinirSenha(jwt.getSubject(), dto.novaSenha());
    }

    private void validarSenhaAtual(String username, String senhaAtual) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakProperties.publicClientId());
        form.add("grant_type", "password");
        form.add("username", username);
        form.add("password", senhaAtual);

        try {
            restClient.post()
                    .uri(keycloakProperties.tokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() != 400 && ex.getStatusCode().value() != 401) {
                throw traduzirErroKeycloak(ex);
            }
            throw new IllegalArgumentException("Invalid current password.");
        }
    }

    private void redefinirSenha(String id, String novaSenha) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "password");
        payload.put("value", novaSenha);
        payload.put("temporary", false);
        put("/users/" + id + "/reset-password", payload);
    }

    private void updateRoles(String id, List<String> roles) {
        if (roles == null) {
            return;
        }

        List<Map<String, Object>> atuais = getList("/users/" + id + "/role-mappings/realm");
        List<Map<String, Object>> atuaisGerenciadas = atuais.stream()
                .filter(role -> ROLES_GERENCIADAS.contains(role.get("name")))
                .toList();

        if (!atuaisGerenciadas.isEmpty()) {
            executarKeycloak(() -> restClient.method(HttpMethod.DELETE)
                    .uri(keycloakProperties.adminRealmUrl() + "/users/" + id + "/role-mappings/realm")
                    .header(HttpHeaders.AUTHORIZATION, bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(atuaisGerenciadas)
                    .retrieve()
                    .toBodilessEntity());
        }

        List<Map<String, Object>> novas = roles.stream()
                .filter(ROLES_GERENCIADAS::contains)
                .map(this::buscarRole)
                .filter(Objects::nonNull)
                .toList();

        if (!novas.isEmpty()) {
            executarKeycloak(() -> restClient.post()
                    .uri(keycloakProperties.adminRealmUrl() + "/users/" + id + "/role-mappings/realm")
                    .header(HttpHeaders.AUTHORIZATION, bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(novas)
                    .retrieve()
                    .toBodilessEntity());
        }
    }

    private Map<String, Object> buscarRole(String role) {
        return getMap("/roles/" + role.trim());
    }

    private List<String> listUserRoles(String id) {
        return getList("/users/" + id + "/role-mappings/realm").stream()
                .map(role -> (String) role.get("name"))
                .filter(Objects::nonNull)
                .filter(ROLES_GERENCIADAS::contains)
                .sorted()
                .toList();
    }

    private Map<String, Object> fetchUserMap(String id) {
        try {
            return getMap("/users/" + id);
        } catch (IdentityException ex) {
            if (ex.getStatus() != HttpStatus.NOT_FOUND) {
                throw ex;
            }
            throw new EntityNotFoundException("User not found.");
        }
    }

    private void put(String path, Map<String, Object> payload) {
        executarKeycloak(() -> restClient.put()
                .uri(keycloakProperties.adminRealmUrl() + path)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(String path) {
        Map<String, Object> response = executarKeycloak(() -> restClient.get()
                .uri(keycloakProperties.adminRealmUrl() + path)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve()
                .body(Map.class));

        return response == null ? Map.of() : response;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(String path) {
        List<Map<String, Object>> response = executarKeycloak(() -> restClient.get()
                .uri(keycloakProperties.adminRealmUrl() + path)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve()
                .body(List.class));

        return response == null ? List.of() : response;
    }

    private String bearer() {
        return "Bearer " + adminAccessToken();
    }

    @SuppressWarnings("unchecked")
    private String adminAccessToken() {
        if (!isBlank(keycloakProperties.adminClientSecret())) {
            return adminClientCredentialsAccessToken();
        }

        if (isBlank(keycloakProperties.adminUsername()) || isBlank(keycloakProperties.adminPassword())) {
            throw new IllegalStateException("Keycloak administrative credentials not configured.");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakProperties.adminClientId());
        form.add("grant_type", "password");
        form.add("username", keycloakProperties.adminUsername());
        form.add("password", keycloakProperties.adminPassword());

        Map<String, Object> response = executarKeycloak(() -> restClient.post()
                .uri(keycloakProperties.adminTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class));

        if (response == null || response.get("access_token") == null) {
            throw new ExternalIntegrationException("Empty administrative response from Keycloak.");
        }

        return (String) response.get("access_token");
    }

    @SuppressWarnings("unchecked")
    private String adminClientCredentialsAccessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakProperties.adminClientId());
        form.add("grant_type", "client_credentials");
        form.add("client_secret", keycloakProperties.adminClientSecret());

        Map<String, Object> response = executarKeycloak(() -> restClient.post()
                .uri(keycloakProperties.tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class));

        if (response == null || response.get("access_token") == null) {
            throw new ExternalIntegrationException("Empty administrative response from Keycloak.");
        }

        return (String) response.get("access_token");
    }

    private UserResponseDTO toUserResponse(Map<String, Object> user, List<String> roles) {
        return new UserResponseDTO(
                (String) user.get("id"),
                (String) user.get("username"),
                (String) user.get("firstName"),
                (String) user.get("lastName"),
                (String) user.get("email"),
                Boolean.TRUE.equals(user.get("enabled")),
                new ArrayList<>(roles)
        );
    }

    private MeuPerfilResponseDTO toMeuPerfil(UserResponseDTO user) {
        return new MeuPerfilResponseDTO(
                user.id(),
                user.username(),
                user.firstName(),
                user.lastName(),
                user.email(),
                user.roles(),
                keycloakProperties.accountUrl()
        );
    }

    private String extrairIdCriado(URI location) {
        if (location == null) {
            throw new ExternalIntegrationException("Keycloak did not return the identifier of the created user.");
        }

        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private String trimToNull(String valor) {
        if (valor == null || valor.trim().isBlank()) {
            return null;
        }

        return valor.trim();
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.isBlank();
    }

    private String firstNonBlank(String... valores) {
        for (String valor : valores) {
            if (!isBlank(valor)) {
                return valor;
            }
        }
        return null;
    }

    private List<String> rolesFromJwt(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return List.of();
        }

        Object rolesObject = realmAccess.get("roles");
        if (!(rolesObject instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(ROLES_GERENCIADAS::contains)
                .sorted()
                .toList();
    }

    private <T> T executarKeycloak(Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (RestClientResponseException ex) {
            throw traduzirErroKeycloak(ex);
        } catch (ResourceAccessException ex) {
            throw new IdentityException(HttpStatus.BAD_GATEWAY, "Identity service unavailable. Please try again in a moment.", ex);
        }
    }

    private IdentityException traduzirErroKeycloak(RestClientResponseException ex) {
        HttpStatus status = switch (ex.getStatusCode().value()) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 404 -> HttpStatus.NOT_FOUND;
            case 409 -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_GATEWAY;
        };

        String message = switch (status) {
            case BAD_REQUEST -> "Date rejected by the identity provider.";
            case NOT_FOUND -> "Identity resource not found.";
            case CONFLICT -> "User already exists or there is a conflict in the identity provider.";
            default -> "Identity service unavailable. Please try again in a moment.";
        };

        return new IdentityException(status, message, ex);
    }
}

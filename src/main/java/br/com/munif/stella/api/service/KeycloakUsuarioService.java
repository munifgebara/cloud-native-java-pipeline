package br.com.munif.stella.api.service;

import br.com.munif.stella.api.config.KeycloakProperties;
import br.com.munif.stella.api.dto.AlterarSenhaDTO;
import br.com.munif.stella.api.dto.MeuPerfilResponseDTO;
import br.com.munif.stella.api.dto.MeuPerfilUpdateDTO;
import br.com.munif.stella.api.dto.UsuarioCreateDTO;
import br.com.munif.stella.api.dto.UsuarioResponseDTO;
import br.com.munif.stella.api.dto.UsuarioUpdateDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class KeycloakUsuarioService {

    private static final List<String> ROLES_GERENCIADAS = List.of("admin", "proprietario", "usuario");

    private final RestClient restClient;
    private final KeycloakProperties keycloakProperties;

    public KeycloakUsuarioService(KeycloakProperties keycloakProperties, RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
        this.keycloakProperties = keycloakProperties;
    }

    public List<UsuarioResponseDTO> listar() {
        List<Map<String, Object>> usuarios = getList("/users?briefRepresentation=false");
        return usuarios.stream()
                .map(usuario -> toUsuarioResponse(usuario, listarRolesDoUsuario((String) usuario.get("id"))))
                .toList();
    }

    public UsuarioResponseDTO buscarPorId(String id) {
        return toUsuarioResponse(buscarUsuarioMap(id), listarRolesDoUsuario(id));
    }

    public UsuarioResponseDTO criar(UsuarioCreateDTO dto) {
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

        ResponseEntity<Void> response = restClient.post()
                .uri(keycloakProperties.adminRealmUrl() + "/users")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();

        String id = extrairIdCriado(response.getHeaders().getLocation());
        atualizarRoles(id, dto.roles());
        return buscarPorId(id);
    }

    public UsuarioResponseDTO atualizar(String id, UsuarioUpdateDTO dto) {
        Map<String, Object> usuario = buscarUsuarioMap(id);
        usuario.put("firstName", trimToNull(dto.firstName()));
        usuario.put("lastName", trimToNull(dto.lastName()));
        usuario.put("email", trimToNull(dto.email()));

        if (dto.enabled() != null) {
            usuario.put("enabled", dto.enabled());
        }

        put("/users/" + id, usuario);
        atualizarRoles(id, dto.roles());
        return buscarPorId(id);
    }

    public void alterarStatus(String id, boolean enabled) {
        Map<String, Object> usuario = buscarUsuarioMap(id);
        usuario.put("enabled", enabled);
        put("/users/" + id, usuario);
    }

    public MeuPerfilResponseDTO meuPerfil(Jwt jwt) {
        UsuarioResponseDTO usuario = buscarPorId(jwt.getSubject());
        return toMeuPerfil(usuario);
    }

    public MeuPerfilResponseDTO atualizarMeuPerfil(Jwt jwt, MeuPerfilUpdateDTO dto) {
        Map<String, Object> usuario = buscarUsuarioMap(jwt.getSubject());
        usuario.put("firstName", trimToNull(dto.firstName()));
        usuario.put("lastName", trimToNull(dto.lastName()));
        usuario.put("email", trimToNull(dto.email()));
        put("/users/" + jwt.getSubject(), usuario);
        return meuPerfil(jwt);
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
        } catch (HttpClientErrorException ex) {
            throw new IllegalArgumentException("Senha atual inválida.");
        }
    }

    private void redefinirSenha(String id, String novaSenha) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "password");
        payload.put("value", novaSenha);
        payload.put("temporary", false);
        put("/users/" + id + "/reset-password", payload);
    }

    private void atualizarRoles(String id, List<String> roles) {
        if (roles == null) {
            return;
        }

        List<Map<String, Object>> atuais = getList("/users/" + id + "/role-mappings/realm");
        List<Map<String, Object>> atuaisGerenciadas = atuais.stream()
                .filter(role -> ROLES_GERENCIADAS.contains(role.get("name")))
                .toList();

        if (!atuaisGerenciadas.isEmpty()) {
            restClient.method(HttpMethod.DELETE)
                    .uri(keycloakProperties.adminRealmUrl() + "/users/" + id + "/role-mappings/realm")
                    .header(HttpHeaders.AUTHORIZATION, bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(atuaisGerenciadas)
                    .retrieve()
                    .toBodilessEntity();
        }

        List<Map<String, Object>> novas = roles.stream()
                .filter(ROLES_GERENCIADAS::contains)
                .map(this::buscarRole)
                .filter(Objects::nonNull)
                .toList();

        if (!novas.isEmpty()) {
            restClient.post()
                    .uri(keycloakProperties.adminRealmUrl() + "/users/" + id + "/role-mappings/realm")
                    .header(HttpHeaders.AUTHORIZATION, bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(novas)
                    .retrieve()
                    .toBodilessEntity();
        }
    }

    private Map<String, Object> buscarRole(String role) {
        return getMap("/roles/" + role.trim());
    }

    private List<String> listarRolesDoUsuario(String id) {
        return getList("/users/" + id + "/role-mappings/realm").stream()
                .map(role -> (String) role.get("name"))
                .filter(Objects::nonNull)
                .filter(ROLES_GERENCIADAS::contains)
                .sorted()
                .toList();
    }

    private Map<String, Object> buscarUsuarioMap(String id) {
        try {
            return getMap("/users/" + id);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new EntityNotFoundException("Usuário não encontrado.");
        }
    }

    private void put(String path, Map<String, Object> payload) {
        restClient.put()
                .uri(keycloakProperties.adminRealmUrl() + path)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(String path) {
        Map<String, Object> response = restClient.get()
                .uri(keycloakProperties.adminRealmUrl() + path)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve()
                .body(Map.class);

        return response == null ? Map.of() : response;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(String path) {
        List<Map<String, Object>> response = restClient.get()
                .uri(keycloakProperties.adminRealmUrl() + path)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve()
                .body(List.class);

        return response == null ? List.of() : response;
    }

    private String bearer() {
        return "Bearer " + adminAccessToken();
    }

    @SuppressWarnings("unchecked")
    private String adminAccessToken() {
        if (isBlank(keycloakProperties.adminUsername()) || isBlank(keycloakProperties.adminPassword())) {
            throw new IllegalStateException("Credenciais administrativas do Keycloak não configuradas.");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakProperties.adminClientId());
        form.add("grant_type", "password");
        form.add("username", keycloakProperties.adminUsername());
        form.add("password", keycloakProperties.adminPassword());

        Map<String, Object> response = restClient.post()
                .uri(keycloakProperties.adminTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new IllegalStateException("Resposta administrativa vazia do Keycloak.");
        }

        return (String) response.get("access_token");
    }

    private UsuarioResponseDTO toUsuarioResponse(Map<String, Object> usuario, List<String> roles) {
        return new UsuarioResponseDTO(
                (String) usuario.get("id"),
                (String) usuario.get("username"),
                (String) usuario.get("firstName"),
                (String) usuario.get("lastName"),
                (String) usuario.get("email"),
                Boolean.TRUE.equals(usuario.get("enabled")),
                new ArrayList<>(roles)
        );
    }

    private MeuPerfilResponseDTO toMeuPerfil(UsuarioResponseDTO usuario) {
        return new MeuPerfilResponseDTO(
                usuario.id(),
                usuario.username(),
                usuario.firstName(),
                usuario.lastName(),
                usuario.email(),
                usuario.roles(),
                keycloakProperties.accountUrl()
        );
    }

    private String extrairIdCriado(URI location) {
        if (location == null) {
            throw new IllegalStateException("Keycloak não retornou o identificador do usuário criado.");
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
}

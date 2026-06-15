package br.com.munif.stella.api.controller;

import br.com.munif.stella.api.dto.AlterarSenhaDTO;
import br.com.munif.stella.api.dto.MeuPerfilResponseDTO;
import br.com.munif.stella.api.dto.MeuPerfilUpdateDTO;
import br.com.munif.stella.api.dto.UsuarioCreateDTO;
import br.com.munif.stella.api.dto.UsuarioResponseDTO;
import br.com.munif.stella.api.dto.UsuarioUpdateDTO;
import br.com.munif.stella.api.service.KeycloakUsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controller REST para gerenciamento de usuários via Keycloak.
 *
 * <p>Expõe o recurso {@code /api/v0/usuarios} com operações administrativas
 * (restritas ao role {@code admin}) e operações do próprio usuário ({@code /me}).</p>
 *
 * <p>Todas as alterações são propagadas diretamente ao Keycloak via Admin API,
 * sem persistência local.</p>
 */
@RestController
@RequestMapping("/api/v0/usuarios")
public class UsuarioController {

    private final KeycloakUsuarioService service;

    /**
     * Constrói o controller injetando o serviço de usuários do Keycloak.
     *
     * @param service serviço responsável pelas operações na Admin API do Keycloak
     */
    public UsuarioController(KeycloakUsuarioService service) {
        this.service = service;
    }

    /**
     * Lista todos os usuários cadastrados no Keycloak. Requer role {@code admin}.
     *
     * @return {@code 200 OK} com a lista de usuários
     */
    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<List<UsuarioResponseDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    /**
     * Busca um usuário pelo seu ID no Keycloak. Requer role {@code admin}.
     *
     * @param id ID do usuário no Keycloak (UUID como string)
     * @return {@code 200 OK} com os dados do usuário
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    /**
     * Cria um novo usuário no Keycloak. Requer role {@code admin}.
     *
     * @param dto dados do novo usuário validados pelo Bean Validation
     * @return {@code 201 Created} com os dados do usuário criado
     */
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<UsuarioResponseDTO> criar(@RequestBody @Valid UsuarioCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    /**
     * Atualiza os dados de um usuário no Keycloak. Requer role {@code admin}.
     *
     * @param id  ID do usuário no Keycloak
     * @param dto dados de atualização validados pelo Bean Validation
     * @return {@code 200 OK} com os dados atualizados do usuário
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<UsuarioResponseDTO> atualizar(@PathVariable String id, @RequestBody @Valid UsuarioUpdateDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    /**
     * Ativa ou desativa um usuário no Keycloak. Requer role {@code admin}.
     *
     * @param id   ID do usuário no Keycloak
     * @param body mapa com a chave {@code "enabled"} e valor booleano
     * @return {@code 204 No Content} em caso de sucesso
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Void> alterarStatus(@PathVariable String id, @RequestBody Map<String, Boolean> body) {
        service.alterarStatus(id, Boolean.TRUE.equals(body.get("enabled")));
        return ResponseEntity.noContent().build();
    }

    /**
     * Retorna o perfil do usuário autenticado, incluindo nome, e-mail e roles.
     *
     * @param jwt token JWT do usuário autenticado
     * @return {@code 200 OK} com os dados do perfil
     */
    @GetMapping("/me")
    public ResponseEntity<MeuPerfilResponseDTO> meuPerfil(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.meuPerfil(jwt));
    }

    /**
     * Atualiza o nome e o e-mail do usuário autenticado no Keycloak.
     *
     * @param jwt token JWT do usuário autenticado
     * @param dto dados de atualização do perfil validados pelo Bean Validation
     * @return {@code 200 OK} com o perfil atualizado
     */
    @PutMapping("/me")
    public ResponseEntity<MeuPerfilResponseDTO> atualizarMeuPerfil(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid MeuPerfilUpdateDTO dto
    ) {
        return ResponseEntity.ok(service.atualizarMeuPerfil(jwt, dto));
    }

    /**
     * Altera a senha do usuário autenticado no Keycloak.
     *
     * @param jwt token JWT do usuário autenticado
     * @param dto DTO com a senha atual e a nova senha validados pelo Bean Validation
     * @return {@code 204 No Content} em caso de sucesso
     */
    @PutMapping("/me/senha")
    public ResponseEntity<Void> alterarMinhaSenha(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid AlterarSenhaDTO dto
    ) {
        service.alterarMinhaSenha(jwt, dto);
        return ResponseEntity.noContent().build();
    }
}

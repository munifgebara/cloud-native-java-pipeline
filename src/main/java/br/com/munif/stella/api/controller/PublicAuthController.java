package br.com.munif.stella.api.controller;

import br.com.munif.stella.api.dto.LoginRequestDTO;
import br.com.munif.stella.api.dto.LoginResponseDTO;
import br.com.munif.stella.api.service.KeycloakLoginService;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST público para autenticação via Keycloak.
 *
 * <p>Expõe o endpoint {@code POST /api/public/login} sem exigir token JWT,
 * permitindo que clientes troquem credenciais (usuário e senha) por um
 * token de acesso Keycloak.</p>
 */
@RestController
@RequestMapping("/api/public")
public class PublicAuthController {

    private final KeycloakLoginService keycloakLoginService;

    /**
     * Constrói o controller injetando o serviço de login do Keycloak.
     *
     * @param keycloakLoginService serviço responsável por autenticar no Keycloak
     */
    public PublicAuthController(KeycloakLoginService keycloakLoginService) {
        this.keycloakLoginService = keycloakLoginService;
    }

    /**
     * Autentica um usuário no Keycloak e retorna o token de acesso.
     *
     * @param request credenciais do usuário (username e password)
     * @return DTO com o token de acesso, token de refresh e demais metadados
     */
    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO request) {
        return keycloakLoginService.login(request);
    }
}
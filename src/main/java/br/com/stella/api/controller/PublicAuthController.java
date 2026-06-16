package br.com.stella.api.controller;

import br.com.stella.api.dto.LoginRequestDTO;
import br.com.stella.api.dto.LoginResponseDTO;
import br.com.stella.api.service.KeycloakLoginService;
import org.springframework.web.bind.annotation.*;

/**
 * Public REST controller for authentication via Keycloak.
 *
 * <p>Exposes the {@code POST /api/public/login} endpoint without requiring a JWT token,
 * allowing clients to exchange credentials (username and password) for a
 * Keycloak access token.</p>
 */
@RestController
@RequestMapping("/api/public")
public class PublicAuthController {

    private final KeycloakLoginService keycloakLoginService;

    /**
     * Constructs the controller injecting the Keycloak login service.
     *
     * @param keycloakLoginService service responsible for authenticating with Keycloak
     */
    public PublicAuthController(KeycloakLoginService keycloakLoginService) {
        this.keycloakLoginService = keycloakLoginService;
    }

    /**
     * Authenticates a user with Keycloak and returns the access token.
     *
     * @param request user credentials (username and password)
     * @return DTO with the access token, refresh token and other metadata
     */
    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO request) {
        return keycloakLoginService.login(request);
    }
}
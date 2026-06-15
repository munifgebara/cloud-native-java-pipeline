package br.com.munif.stella.api.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST para consulta de informações do usuário autenticado via JWT.
 *
 * <p>O endpoint {@code GET /api/me} retorna os claims do token JWT do usuário
 * atualmente autenticado, incluindo subject, nome de usuário, nome completo,
 * e-mail e roles do Keycloak.</p>
 */
@RestController
public class AuthController {

    /**
     * Retorna os dados do usuário autenticado extraídos do token JWT.
     *
     * @param jwt token JWT injetado pelo Spring Security
     * @return mapa com subject, username, name, email e realm_access
     */
    @GetMapping("/api/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("subject", jwt.getSubject());
        response.put("username", jwt.getClaimAsString("preferred_username"));
        response.put("name", jwt.getClaimAsString("name"));
        response.put("email", jwt.getClaimAsString("email"));
        response.put("realm_access", jwt.getClaim("realm_access"));
        return response;
    }
}
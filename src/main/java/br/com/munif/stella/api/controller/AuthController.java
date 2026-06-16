package br.com.munif.stella.api.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for querying information about the authenticated user via JWT.
 *
 * <p>The {@code GET /api/me} endpoint returns the JWT token claims of the currently
 * authenticated user, including subject, username, full name, e-mail and Keycloak roles.</p>
 */
@RestController
public class AuthController {

    /**
     * Returns the authenticated user's data extracted from the JWT token.
     *
     * @param jwt JWT token injected by Spring Security
     * @return map with subject, username, name, email and realm_access
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
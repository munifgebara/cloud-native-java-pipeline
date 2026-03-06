package br.com.munif.pagadoria.api.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

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
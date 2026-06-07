package br.com.munif.stella.api.dto;

import java.util.List;

public record UsuarioResponseDTO(
        String id,
        String username,
        String firstName,
        String lastName,
        String email,
        boolean enabled,
        List<String> roles
) {}

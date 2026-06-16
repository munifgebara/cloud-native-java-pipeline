package br.com.stella.api.dto;

import java.util.List;

public record UserResponseDTO(
        String id,
        String username,
        String firstName,
        String lastName,
        String email,
        boolean enabled,
        List<String> roles
) {}

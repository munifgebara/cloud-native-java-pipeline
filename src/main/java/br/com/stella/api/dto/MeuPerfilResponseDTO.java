package br.com.stella.api.dto;

import java.util.List;

public record MeuPerfilResponseDTO(
        String id,
        String username,
        String firstName,
        String lastName,
        String email,
        List<String> roles,
        String alteracaoSenhaUrl
) {}

package br.com.stella.api.dto;

import java.util.List;

public record MyProfileResponseDTO(
        String id,
        String username,
        String firstName,
        String lastName,
        String email,
        List<String> roles,
        String pictureUrl,
        String passwordChangeUrl
) {}

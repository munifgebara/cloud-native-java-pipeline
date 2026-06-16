package br.com.munif.stella.api.dto;

/**
 * Successful authentication response DTO.
 *
 * <p>Returned after a successful authentication with the user's credentials.
 * The {@code accessToken} must be sent in the {@code Authorization: Bearer <token>} header
 * in all authenticated requests. The {@code refreshToken} allows renewing
 * access without new credentials, until it expires.</p>
 *
 * @param accessToken  JWT access token for authenticating API requests
 * @param refreshToken token for renewing the {@code accessToken} without re-authentication
 * @param tokenType    token type; typically {@code "Bearer"}
 * @param expiresIn    expiration time of the {@code accessToken} in seconds
 */
public record LoginResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn
) {
}

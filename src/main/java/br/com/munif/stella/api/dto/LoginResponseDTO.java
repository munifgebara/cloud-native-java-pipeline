package br.com.munif.stella.api.dto;

/**
 * DTO de resposta de autenticação bem-sucedida.
 *
 * <p>Retornado após uma autenticação bem-sucedida com as credenciais do usuário.
 * O {@code accessToken} deve ser enviado no cabeçalho {@code Authorization: Bearer <token>}
 * em todas as requisições autenticadas. O {@code refreshToken} permite renovar
 * o acesso sem novas credenciais, até que expire.</p>
 *
 * @param accessToken  token JWT de acesso para autenticar requisições à API
 * @param refreshToken token para renovar o {@code accessToken} sem reautenticação
 * @param tokenType    tipo do token; normalmente {@code "Bearer"}
 * @param expiresIn    tempo de expiração do {@code accessToken} em segundos
 */
public record LoginResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn
) {
}

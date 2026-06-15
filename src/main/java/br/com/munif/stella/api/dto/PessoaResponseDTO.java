package br.com.munif.stella.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO completo de resposta de uma pessoa.
 *
 * <p>Retornado nas operações de criação, atualização e consulta individual.
 * Inclui todos os dados cadastrais da pessoa, incluindo endereço completo
 * e timestamps de auditoria.</p>
 *
 * @param id                 identificador único da pessoa
 * @param nome               nome completo ou razão social
 * @param cpfCnpj            CPF ou CNPJ (somente dígitos)
 * @param telefonePrincipal  telefone principal de contato; pode ser {@code null}
 * @param telefoneSecundario telefone alternativo; pode ser {@code null}
 * @param email              endereço de e-mail; pode ser {@code null}
 * @param cep                CEP do endereço (somente dígitos); pode ser {@code null}
 * @param endereco           logradouro e número; pode ser {@code null}
 * @param complemento        complemento do endereço; pode ser {@code null}
 * @param bairro             bairro; pode ser {@code null}
 * @param cidade             cidade; pode ser {@code null}
 * @param uf                 sigla do estado (2 letras); pode ser {@code null}
 * @param criadoEm           data e hora de criação do registro (UTC)
 * @param alteradoEm         data e hora da última alteração do registro (UTC)
 */
public record PessoaResponseDTO(
        UUID id,
        String nome,
        String cpfCnpj,
        String telefonePrincipal,
        String telefoneSecundario,
        String email,
        String cep,
        String endereco,
        String complemento,
        String bairro,
        String cidade,
        String uf,
        Instant criadoEm,
        Instant alteradoEm
) {}

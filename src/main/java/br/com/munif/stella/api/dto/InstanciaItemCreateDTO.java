package br.com.munif.stella.api.dto;

import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO de criação de uma instância de item.
 *
 * <p>Contém os dados necessários para registrar uma nova instância física de um
 * {@code ItemMestre} no sistema, incluindo identificação, localização inicial
 * e status operacional de partida.</p>
 *
 * @param itemMestreId      identificador do item mestre ao qual esta instância pertence; obrigatório
 * @param localAtualId      identificador do local de armazenamento inicial da instância; opcional
 * @param identificador     código interno de identificação da instância (até 100 caracteres); opcional
 * @param patrimonio        número de patrimônio atribuído ao bem (até 100 caracteres); opcional
 * @param numeroSerie       número de série do fabricante gravado no equipamento (até 150 caracteres); opcional
 * @param statusOperacional status operacional inicial; quando {@code null}, assume {@code DISPONIVEL}
 * @param observacoes       observações internas sobre esta instância (até 1000 caracteres); opcional
 * @param origemCadastro    origem do cadastro (ex.: {@code "MANUAL"}, {@code "FOTO"}); até 50 caracteres; opcional
 * @param ativa             indica se a instância deve ser criada ativa; quando {@code null}, assume {@code true}
 */
public record InstanciaItemCreateDTO(
        @NotNull(message = "Main item is required.")
        UUID itemMestreId,

        UUID localAtualId,

        @Size(max = 100, message = "Identifier must not exceed 100 characters.")
        String identificador,

        @Size(max = 100, message = "Asset number must not exceed 100 characters.")
        String patrimonio,

        @Size(max = 150, message = "Serial number must not exceed 150 characters.")
        String numeroSerie,

        StatusOperacionalInstancia statusOperacional,

        @Size(max = 1000, message = "Notes must not exceed 1000 characters.")
        String observacoes,

        @Size(max = 50, message = "Registration origin must not exceed 50 characters.")
        String origemCadastro,

        Boolean ativa
) {}

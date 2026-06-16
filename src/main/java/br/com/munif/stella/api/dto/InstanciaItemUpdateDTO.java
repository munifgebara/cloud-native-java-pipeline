package br.com.munif.stella.api.dto;

import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO de atualização de uma instância de item.
 *
 * <p>Contém os campos que podem ser alterados em uma instância já cadastrada.
 * Todos os campos (exceto {@code itemMestreId}) são opcionais: valores {@code null}
 * limpam o campo correspondente na entidade.</p>
 *
 * @param itemMestreId      identificador do item mestre ao qual esta instância pertence; obrigatório
 * @param localAtualId      identificador do novo local de armazenamento; {@code null} remove a localização
 * @param identificador     código interno de identificação (até 100 caracteres); opcional
 * @param patrimonio        número de patrimônio (até 100 caracteres); opcional
 * @param numeroSerie       número de série do fabricante (até 150 caracteres); opcional
 * @param statusOperacional novo status operacional da instância; opcional
 * @param observacoes       observações internas (até 1000 caracteres); opcional
 * @param origemCadastro    origem do cadastro (até 50 caracteres); opcional
 * @param ativa             indica se a instância está ativa; opcional
 */
public record InstanciaItemUpdateDTO(
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

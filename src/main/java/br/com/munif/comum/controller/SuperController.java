package br.com.munif.comum.controller;

import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

/**
 * Contrato base para os controllers REST do sistema.
 *
 * <p>Define os endpoints padrão de CRUD que devem ser implementados por cada
 * controller concreto. O uso de parâmetros de tipo genéricos permite que cada
 * recurso exponha seus próprios DTOs (resumo, resposta, criação e atualização)
 * sem duplicar a estrutura de métodos.</p>
 *
 * <p><strong>Convenção de nomes dos parâmetros de tipo:</strong></p>
 * <ul>
 *   <li>{@code RESUMO}   — DTO leve retornado em listagens (ex.: {@code ItemMestreResumoDTO})</li>
 *   <li>{@code RESPONSE} — DTO completo retornado em buscas individuais e operações de escrita</li>
 *   <li>{@code CREATE}   — DTO de entrada para criação de um novo registro</li>
 *   <li>{@code UPDATE}   — DTO de entrada para atualização de um registro existente</li>
 *   <li>{@code ENTITY}   — tipo da entidade JPA (necessário para tipagem do histórico Envers)</li>
 * </ul>
 *
 * @param <RESUMO>   DTO de resumo usado em listagens
 * @param <RESPONSE> DTO completo retornado em operações de leitura/escrita individuais
 * @param <CREATE>   DTO de criação
 * @param <UPDATE>   DTO de atualização
 * @param <ENTITY>   tipo da entidade JPA
 */
public abstract class SuperController<RESUMO, RESPONSE, CREATE, UPDATE, ENTITY> {

    /**
     * Cria um novo registro.
     *
     * @param dto dados de criação validados pelo Bean Validation
     * @return {@code 201 Created} com o DTO completo do registro criado
     */
    public abstract ResponseEntity<RESPONSE> criar(CREATE dto);

    /**
     * Retorna os dados completos de um registro pelo seu identificador.
     *
     * @param id identificador UUID do registro
     * @return {@code 200 OK} com o DTO completo; {@code 404 Not Found} se não existir
     */
    public abstract ResponseEntity<RESPONSE> buscarPorId(UUID id);

    /**
     * Retorna a listagem resumida de todos os registros ativos.
     *
     * @return {@code 200 OK} com a lista de DTOs de resumo
     */
    public abstract ResponseEntity<List<RESUMO>> listar();

    /**
     * Atualiza um registro existente.
     *
     * @param id  identificador UUID do registro a atualizar
     * @param dto dados de atualização validados pelo Bean Validation
     * @return {@code 200 OK} com o DTO completo atualizado; {@code 404} se não existir
     */
    public abstract ResponseEntity<RESPONSE> atualizar(UUID id, UPDATE dto);

    /**
     * Realiza a exclusão lógica de um registro (define {@code ativo = false}).
     *
     * @param id identificador UUID do registro a inativar
     * @return {@code 204 No Content} após a inativação; {@code 404} se não existir
     */
    public abstract ResponseEntity<Void> excluir(UUID id);

    /**
     * Retorna todos os registros, incluindo os inativados logicamente.
     * Útil para telas administrativas que precisam visualizar o histórico completo.
     *
     * @return {@code 200 OK} com a lista completa de DTOs de resumo
     */
    public abstract ResponseEntity<List<RESUMO>> listarTodosIncluindoInativos();

    /**
     * Retorna o histórico de revisões anteriores de um registro (Hibernate Envers).
     *
     * @param id identificador UUID do registro
     * @return {@code 200 OK} com a lista de revisões em ordem cronológica;
     *         lista vazia se não houver histórico
     */
    public abstract ResponseEntity<? extends List<?>> listarVersoesAnteriores(UUID id);
}

package br.com.munif.comum.service;

import br.com.munif.comum.dto.RevisaoDTO;
import br.com.munif.comum.persistencia.Entidade;
import br.com.munif.comum.persistencia.SuperRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Serviço base com operações de persistência comuns a todas as entidades do sistema.
 *
 * <p>Centraliza CRUD genérico e consulta ao histórico de revisões (Hibernate Envers),
 * eliminando código repetido nos serviços concretos.
 * Cada serviço concreto herda desta classe, informando o tipo da entidade ({@code T})
 * e o repositório correspondente ({@code R}).</p>
 *
 * <p><strong>Exclusão lógica:</strong> o método {@link #excluir(UUID)} não remove o
 * registro do banco; apenas define {@code ativo = false} via
 * {@link Entidade#excluirLogicamente()}.</p>
 *
 * <p><strong>Histórico:</strong> {@link #listarVersoesAnteriores(UUID)} retorna snapshots
 * auditados gerados pelo Hibernate Envers, excluindo a revisão atual (que representa o
 * estado presente, já acessível por {@link #buscarPorId(UUID)}).</p>
 *
 * @param <T> tipo da entidade gerenciada
 * @param <R> tipo do repositório, que deve estender {@link SuperRepository}
 */
public abstract class SuperService<T extends Entidade, R extends SuperRepository<T>> {

    /** Repositório Spring Data da entidade gerenciada, disponível para subclasses. */
    protected final R repository;

    /**
     * {@code EntityManager} JPA, necessário para obter o {@link AuditReader} do Envers.
     * Disponível para subclasses que precisem de operações JPA avançadas.
     */
    protected final EntityManager entityManager;

    /** Classe da entidade, usada para consultas Envers com o método genérico {@code find}. */
    private final Class<T> entityClass;

    /**
     * Construtor requerido pelas subclasses para injetar as dependências fundamentais.
     *
     * @param repository   repositório da entidade
     * @param entityManager gerenciador de entidades JPA
     * @param entityClass   classe literal da entidade (ex.: {@code ItemMestre.class})
     */
    protected SuperService(R repository, EntityManager entityManager, Class<T> entityClass) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.entityClass = entityClass;
    }

    /**
     * Persiste ou atualiza uma entidade.
     *
     * @param entidade entidade a salvar
     * @return entidade salva, com campos gerados pelo banco (ex.: {@code id}, {@code version})
     */
    public T salvar(T entidade) {
        return repository.save(entidade);
    }

    /**
     * Retorna todos os registros ativos da entidade.
     *
     * @return lista de entidades ativas, ordenação definida pelo repositório concreto
     */
    public List<T> listarTodos() {
        return repository.findAll();
    }

    /**
     * Retorna todos os registros da entidade, incluindo os inativados logicamente.
     *
     * @return lista completa, independente do campo {@code ativo}
     */
    public List<T> listarTodosIncluindoInativos() {
        return repository.listarTodosIncluindoInativos();
    }

    /**
     * Busca um registro pelo identificador único.
     *
     * @param id identificador UUID do registro
     * @return entidade encontrada
     * @throws EntityNotFoundException se nenhum registro existir com o {@code id} informado
     */
    public T buscarPorId(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Registro não encontrado para id: " + id));
    }

    /**
     * Realiza a exclusão lógica do registro identificado por {@code id}.
     *
     * <p>O registro permanece no banco de dados com {@code ativo = false}
     * e não aparecerá nas listagens padrão.</p>
     *
     * @param id identificador do registro a inativar
     * @throws EntityNotFoundException se o registro não existir
     */
    public void excluir(UUID id) {
        T entidade = buscarPorId(id);
        entidade.excluirLogicamente();
        repository.save(entidade);
    }

    /**
     * Retorna as revisões anteriores de um registro, excluindo a revisão atual.
     *
     * <p>Utiliza o Hibernate Envers para consultar o histórico auditado.
     * Cada item da lista representa um snapshot do estado da entidade em
     * um momento passado.</p>
     *
     * @param id identificador do registro cujo histórico se deseja consultar
     * @return lista de revisões em ordem crescente de número de revisão;
     *         lista vazia se não houver histórico
     */
    public List<RevisaoDTO<T>> listarVersoesAnteriores(UUID id) {
        AuditReader auditReader = AuditReaderFactory.get(entityManager);
        List<Number> revisoes = auditReader.getRevisions(entityClass, id);

        if (revisoes.isEmpty()) {
            return List.of();
        }

        List<RevisaoDTO<T>> resultado = new ArrayList<>();
        // A última revisão representa o estado atual, que já é obtido por buscarPorId().
        // Por isso percorremos apenas até revisoes.size() - 1.
        for (int i = 0; i < revisoes.size() - 1; i++) {
            Number numeroRevisao = revisoes.get(i);
            T entidadeRevisada = auditReader.find(entityClass, id, numeroRevisao);

            if (entidadeRevisada != null) {
                resultado.add(new RevisaoDTO<>(
                        numeroRevisao,
                        auditReader.getRevisionDate(numeroRevisao).toInstant(),
                        entidadeRevisada
                ));
            }
        }

        return resultado;
    }
}

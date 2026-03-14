package br.com.munif.comum.service;

import br.com.munif.comum.dto.RevisaoDTO;
import br.com.munif.comum.persistencia.Entidade;
import br.com.munif.comum.persistencia.SuperRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class SuperService<T extends Entidade, R extends SuperRepository<T>> {

    protected final R repository;
    protected final EntityManager entityManager;
    private final Class<T> entityClass;

    protected SuperService(R repository, EntityManager entityManager, Class<T> entityClass) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.entityClass = entityClass;
    }

    public T salvar(T entidade) {
        return repository.save(entidade);
    }

    public List<T> listarTodos() {
        return repository.findAll();
    }

    public List<T> listarTodosIncluindoInativos() {
        return repository.listarTodosIncluindoInativos();
    }

    public T buscarPorId(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Registro não encontrado para id: " + id));
    }

    public void excluir(UUID id) {
        T entidade = buscarPorId(id);
        entidade.excluirLogicamente();
        repository.save(entidade);
    }

    public List<RevisaoDTO<T>> listarVersoesAnteriores(UUID id) {
        AuditReader auditReader = AuditReaderFactory.get(entityManager);

        List<Number> revisoes = auditReader.getRevisions(entityClass, id);
        List<RevisaoDTO<T>> resultado = new ArrayList<>();

        if (revisoes.isEmpty()) {
            return resultado;
        }

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

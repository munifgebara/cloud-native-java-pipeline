package br.com.munif.common.persistencia;

import br.com.munif.common.owner.OwnerContext;
import br.com.munif.common.owner.OwnerIdentity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OwnedJpaRepository<T extends BaseEntity> extends SimpleJpaRepository<T, UUID>
        implements SuperRepository<T> {

    private final JpaEntityInformation<T, UUID> entityInformation;
    private final EntityManager entityManager;

    public OwnedJpaRepository(JpaEntityInformation<T, UUID> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<T> findById(UUID id) {
        return super.findOne(owned((root, query, cb) -> cb.equal(root.get("id"), id)));
    }

    @Override
    public List<T> findAll() {
        return super.findAll(owned(null));
    }

    @Override
    public List<T> findAll(Sort sort) {
        return super.findAll(owned(null), sort);
    }

    @Override
    public List<T> findAll(Specification<T> spec) {
        return super.findAll(owned(spec));
    }

    @Override
    public List<T> findAll(Specification<T> spec, Sort sort) {
        return super.findAll(owned(spec), sort);
    }

    @Override
    public long count() {
        return super.count(owned(null));
    }

    @Override
    public long count(Specification<T> spec) {
        return super.count(owned(spec));
    }

    @Override
    public List<T> findAllIncludingInactive() {
        return super.findAll(owned(null));
    }

    @Override
    public List<T> findAllActive(Sort sort) {
        return super.findAll(owned(active()), sort);
    }

    @Override
    public long countActive() {
        return super.count(owned(active()));
    }

    @Override
    public <S extends T> S save(S entity) {
        if (entity.isNew()) {
            assignOwner(entity);
        } else {
            verifyWritable(entity);
        }
        return super.save(entity);
    }

    private void assignOwner(T entity) {
        if (entity.getOwnerEmail() != null && entity.getOwnerIssuer() != null) {
            return;
        }
        OwnerContext.current().ifPresent(owner -> {
            entity.setOwnerEmail(owner.email());
            entity.setOwnerIssuer(owner.issuer());
        });
    }

    private void verifyWritable(T entity) {
        OwnerContext.current().ifPresent(owner -> {
            UUID id = entityInformation.getId(entity);
            FlushModeType previousFlushMode = entityManager.getFlushMode();
            boolean allowed;
            try {
                entityManager.setFlushMode(FlushModeType.COMMIT);
                allowed = super.exists((root, query, cb) -> cb.and(
                        cb.equal(root.get("id"), id),
                        cb.equal(root.get("ownerEmail"), owner.email()),
                        cb.equal(root.get("ownerIssuer"), owner.issuer())
                ));
            } finally {
                entityManager.setFlushMode(previousFlushMode);
            }
            if (!allowed) {
                entityManager.detach(entity);
                throw new EntityNotFoundException("Record not found for id: " + id);
            }
        });
    }

    private Specification<T> active() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    private Specification<T> owned(Specification<T> spec) {
        return (root, query, cb) -> {
            Predicate base = spec == null ? cb.conjunction() : spec.toPredicate(root, query, cb);
            Optional<OwnerIdentity> owner = OwnerContext.current();
            if (owner.isEmpty()) {
                return base;
            }
            Predicate readableByOwner = cb.and(
                    cb.equal(root.get("ownerEmail"), owner.get().email()),
                    cb.equal(root.get("ownerIssuer"), owner.get().issuer())
            );
            Predicate readablePublic = cb.isTrue(root.get("ownerPublic"));
            return cb.and(base, cb.or(readableByOwner, readablePublic));
        };
    }
}

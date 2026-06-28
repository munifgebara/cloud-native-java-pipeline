package br.com.munif.common.service;

import br.com.munif.common.dto.RevisionDTO;
import br.com.munif.common.persistencia.BaseEntity;
import br.com.munif.common.persistencia.SuperRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuperCrudServiceTest {

    private final TestRepository repository = mock(TestRepository.class);
    private final TestCrudService service = new TestCrudService(repository, mock(EntityManager.class));

    @Test
    void shouldMapOwnerScopedStandardReads() {
        TestEntity entity = entity("Record");
        UUID id = entity.getId();
        Sort sort = Sort.by("name");
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.findAllActive(sort)).thenReturn(List.of(entity));
        when(repository.findAllIncludingInactive()).thenReturn(List.of(entity));

        assertThat(service.findResponseById(id)).isEqualTo("response:Record");
        assertThat(service.listSummary()).containsExactly("summary:Record");
        assertThat(service.listSummaryIncludingInactive()).containsExactly("summary:Record");
    }

    @Test
    void shouldSoftDeleteThroughTheCommonCrudOperation() {
        TestEntity entity = entity("Record");
        UUID id = entity.getId();
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        service.deleteLogically(id);

        assertThat(entity.isActive()).isFalse();
        verify(repository).save(entity);
    }

    private TestEntity entity(String name) {
        TestEntity entity = new TestEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(name);
        return entity;
    }

    private static class TestCrudService extends SuperCrudService<
            TestEntity,
            TestRepository,
            String,
            String,
            String,
            String,
            RevisionDTO<TestEntity>> {

        private TestCrudService(TestRepository repository, EntityManager entityManager) {
            super(repository, entityManager, TestEntity.class);
        }

        @Override
        public String create(String dto) {
            return dto;
        }

        @Override
        public String update(UUID id, String dto) {
            return dto;
        }

        @Override
        protected String toSummary(TestEntity entity) {
            return "summary:" + entity.getName();
        }

        @Override
        protected String toResponse(TestEntity entity) {
            return "response:" + entity.getName();
        }
    }

    private interface TestRepository extends SuperRepository<TestEntity> {
    }

    private static class TestEntity extends BaseEntity {
        private String name;

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }
    }
}

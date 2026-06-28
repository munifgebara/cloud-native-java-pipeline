package br.com.munif.common.service;

import br.com.munif.common.persistencia.BaseEntity;
import br.com.munif.common.persistencia.SuperRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuperServiceTest {

    private final TestRepository repository = mock(TestRepository.class);
    private final TestService service = new TestService(repository, mock(EntityManager.class));

    @Test
    void shouldPersistTwiceAfterInitialFlushWhenInactiveStateIsRequested() {
        TestEntity entity = new TestEntity();
        when(repository.save(entity)).thenReturn(entity);

        TestEntity saved = service.saveWithActiveState(entity, false);

        assertThat(saved.isActive()).isFalse();
        verify(repository).flush();
        verify(repository, times(2)).save(entity);
    }

    @Test
    void shouldPersistOnceWithoutFlushWhenActiveStateIsRequested() {
        TestEntity entity = new TestEntity();
        when(repository.save(entity)).thenReturn(entity);

        TestEntity saved = service.saveWithActiveState(entity, true);

        assertThat(saved.isActive()).isTrue();
        verify(repository, never()).flush();
        verify(repository).save(entity);
    }

    @Test
    void shouldPersistOnceWithoutFlushWhenStateIsNotSpecified() {
        TestEntity entity = new TestEntity();
        when(repository.save(entity)).thenReturn(entity);

        TestEntity saved = service.saveWithActiveState(entity, null);

        assertThat(saved.isActive()).isTrue();
        verify(repository, never()).flush();
        verify(repository).save(entity);
    }

    private static class TestService extends SuperService<TestEntity, TestRepository> {

        private TestService(TestRepository repository, EntityManager entityManager) {
            super(repository, entityManager, TestEntity.class);
        }

        private TestEntity saveWithActiveState(TestEntity entity, Boolean active) {
            return saveWithRequestedActiveState(entity, active);
        }
    }

    private interface TestRepository extends SuperRepository<TestEntity> {
    }

    private static class TestEntity extends BaseEntity {
    }
}

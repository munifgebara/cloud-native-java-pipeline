package br.com.stella.api.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InactiveRecordsPurgeServiceTest {

    @Test
    void shouldDeleteInactiveRecordsInForeignKeyOrder() {
        EntityManager entityManager = mock(EntityManager.class);
        Query loans = query(entityManager, "item_loan", 1);
        Query movements = query(entityManager, "item_movement", 2);
        Query embeddings = query(entityManager, "main_item_embedding", 3);
        Query instances = query(entityManager, "item_instance", 4);
        Query items = query(entityManager, "main_item", 5);
        Query locationParents = mock(Query.class);
        when(entityManager.createNativeQuery("""
                update public.storage_location
                set parent_location_id = null
                where active = false
                """)).thenReturn(locationParents);
        when(locationParents.executeUpdate()).thenReturn(1);
        Query locations = query(entityManager, "storage_location", 6);
        Query categories = query(entityManager, "category", 7);
        Query people = query(entityManager, "person", 8);

        var result = new InactiveRecordsPurgeService(entityManager).purge();

        assertThat(result.total()).isEqualTo(36);
        assertThat(result.itemLoans()).isEqualTo(1);
        assertThat(result.people()).isEqualTo(8);

        InOrder order = inOrder(loans, movements, embeddings, instances, items, locationParents, locations, categories, people);
        order.verify(loans).executeUpdate();
        order.verify(movements).executeUpdate();
        order.verify(embeddings).executeUpdate();
        order.verify(instances).executeUpdate();
        order.verify(items).executeUpdate();
        order.verify(locationParents).executeUpdate();
        order.verify(locations).executeUpdate();
        order.verify(categories).executeUpdate();
        order.verify(people).executeUpdate();
    }

    private Query query(EntityManager entityManager, String table, int deletedRecords) {
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery("delete from public.%s where active = false".formatted(table)))
                .thenReturn(query);
        when(query.executeUpdate()).thenReturn(deletedRecords);
        return query;
    }
}

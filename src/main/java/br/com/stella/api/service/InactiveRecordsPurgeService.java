package br.com.stella.api.service;

import br.com.stella.api.dto.InactiveRecordsPurgeResultDTO;
import br.com.stella.api.observability.StructuredBusinessLogger;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Physically removes inactive operational records in foreign-key dependency order.
 */
@Service
public class InactiveRecordsPurgeService {

    private static final Logger log = LoggerFactory.getLogger(InactiveRecordsPurgeService.class);

    private final EntityManager entityManager;

    public InactiveRecordsPurgeService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public InactiveRecordsPurgeResultDTO purge() {
        int itemLoans = deleteInactive("item_loan");
        int itemMovements = deleteInactive("item_movement");
        int mainItemEmbeddings = deleteInactive("main_item_embedding");
        int itemInstances = deleteInactive("item_instance");
        int mainItems = deleteInactive("main_item");
        clearInactiveLocationParents();
        int storageLocations = deleteInactive("storage_location");
        int categories = deleteInactive("category");
        int people = deleteInactive("person");

        var result = new InactiveRecordsPurgeResultDTO(
                itemLoans,
                itemMovements,
                mainItemEmbeddings,
                itemInstances,
                mainItems,
                storageLocations,
                categories,
                people
        );
        StructuredBusinessLogger.info(log, "maintenance", "inactive-records-purged", StructuredBusinessLogger.fields(
                "deleted_item_loans", itemLoans,
                "deleted_item_movements", itemMovements,
                "deleted_main_item_embeddings", mainItemEmbeddings,
                "deleted_item_instances", itemInstances,
                "deleted_main_items", mainItems,
                "deleted_storage_locations", storageLocations,
                "deleted_categories", categories,
                "deleted_people", people,
                "deleted_total", result.total(),
                "success", true
        ));
        return result;
    }

    private int deleteInactive(String table) {
        return entityManager.createNativeQuery("delete from public.%s where active = false".formatted(table))
                .executeUpdate();
    }

    private void clearInactiveLocationParents() {
        entityManager.createNativeQuery("""
                update public.storage_location
                set parent_location_id = null
                where active = false
                """).executeUpdate();
    }
}

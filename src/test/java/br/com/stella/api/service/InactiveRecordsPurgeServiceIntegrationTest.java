package br.com.stella.api.service;

import br.com.munif.common.persistencia.BaseEntity;
import br.com.stella.api.entity.Category;
import br.com.stella.api.entity.ItemInstance;
import br.com.stella.api.entity.ItemInstanceStatus;
import br.com.stella.api.entity.ItemLoan;
import br.com.stella.api.entity.ItemMovement;
import br.com.stella.api.entity.ItemMovementType;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.entity.Person;
import br.com.stella.api.entity.StorageLocation;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(InactiveRecordsPurgeService.class)
class InactiveRecordsPurgeServiceIntegrationTest {

    private static final String OWNER_EMAIL = "purge-test@example.local";
    private static final String OWNER_ISSUER = "https://issuer.example.local/realms/test";

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private InactiveRecordsPurgeService service;

    @Test
    void shouldPurgeACompleteInactiveGraphWithoutReferentialIntegrityErrors() {
        Category category = owned(new Category());
        category.setName("Categoria inativa para purga");
        Category activeCategory = owned(new Category());
        activeCategory.setName("Categoria ativa preservada");

        StorageLocation parent = owned(new StorageLocation());
        parent.setName("Local pai inativo");
        StorageLocation child = owned(new StorageLocation());
        child.setName("Local filho inativo");
        child.setParent(parent);

        MainItem item = owned(new MainItem());
        item.setName("Item inativo para purga");
        item.setCategory(category);

        ItemInstance instance = owned(new ItemInstance());
        instance.setMainItem(item);
        instance.setCurrentLocation(child);
        instance.setOperationalStatus(ItemInstanceStatus.DISPONIVEL);

        Person person = owned(new Person());
        person.setName("Pessoa inativa para purga");
        person.setTaxId(UUID.randomUUID().toString().replace("-", "").substring(0, 11));

        ItemMovement movement = owned(new ItemMovement());
        movement.setType(ItemMovementType.TRANSFERENCIA);
        movement.setMovementDate(Instant.now());
        movement.setItemInstance(instance);
        movement.setOriginLocation(parent);
        movement.setDestinationLocation(child);

        ItemLoan loan = owned(new ItemLoan());
        loan.setItemInstance(instance);
        loan.setPerson(person);

        persist(category, activeCategory, parent, child, item, instance, person, movement, loan);
        deactivate(category, parent, child, item, instance, person, movement, loan);
        entityManager.flush();
        entityManager.clear();

        var result = service.purge();
        entityManager.clear();

        assertThat(result.total()).isEqualTo(8);
        assertThat(entityManager.find(Category.class, activeCategory.getId())).isNotNull();
        assertThat(countInactiveRecords()).isZero();
    }

    private long countInactiveRecords() {
        return ((Number) entityManager.createNativeQuery("""
                select
                    (select count(*) from public.item_loan where active = false) +
                    (select count(*) from public.item_movement where active = false) +
                    (select count(*) from public.main_item_embedding where active = false) +
                    (select count(*) from public.item_instance where active = false) +
                    (select count(*) from public.main_item where active = false) +
                    (select count(*) from public.storage_location where active = false) +
                    (select count(*) from public.category where active = false) +
                    (select count(*) from public.person where active = false)
                """).getSingleResult()).longValue();
    }

    private void persist(Object... entities) {
        for (Object entity : entities) {
            entityManager.persist(entity);
        }
    }

    private void deactivate(BaseEntity... entities) {
        for (BaseEntity entity : entities) {
            entity.deleteLogically();
        }
    }

    private <T extends BaseEntity> T owned(T entity) {
        entity.setOwnerEmail(OWNER_EMAIL);
        entity.setOwnerIssuer(OWNER_ISSUER);
        return entity;
    }
}

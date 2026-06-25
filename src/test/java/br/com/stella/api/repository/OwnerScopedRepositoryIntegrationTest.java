package br.com.stella.api.repository;

import br.com.munif.common.owner.OwnerContext;
import br.com.munif.common.owner.OwnerIdentity;
import br.com.stella.api.entity.Category;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OwnerScopedRepositoryIntegrationTest {

    private static final OwnerIdentity OWNER_A = new OwnerIdentity("OwnerA@Example.Local", "https://issuer-a/realms/stella");
    private static final OwnerIdentity OWNER_B = new OwnerIdentity("ownerb@example.local", "https://issuer-a/realms/stella");

    @Autowired
    private CategoryRepository categoryRepository;

    @AfterEach
    void clearOwner() {
        OwnerContext.clear();
    }

    @Test
    void shouldAssignOwnerAndHidePrivateRecordsFromOtherOwners() {
        OwnerContext.set(OWNER_A);
        Category category = category("Owner scoped private category", false);
        Category saved = categoryRepository.saveAndFlush(category);

        assertThat(saved.getOwnerEmail()).isEqualTo("ownera@example.local");
        assertThat(saved.getOwnerIssuer()).isEqualTo("https://issuer-a/realms/stella");

        OwnerContext.set(OWNER_B);

        assertThat(categoryRepository.findById(saved.getId())).isEmpty();
        assertThat(categoryRepository.findAll()).extracting(Category::getName)
                .doesNotContain("Owner scoped private category");
    }

    @Test
    void shouldAllowPublicOwnerRecordsToBeReadButNotMutatedByOtherOwners() {
        OwnerContext.set(OWNER_A);
        Category category = category("Owner scoped public category", true);
        Category saved = categoryRepository.saveAndFlush(category);

        OwnerContext.set(OWNER_B);
        Category visible = categoryRepository.findById(saved.getId()).orElseThrow();
        visible.setDescription("Attempted cross-owner update");

        assertThat(visible.isOwnerPublic()).isTrue();
        assertThatThrownBy(() -> categoryRepository.saveAndFlush(visible))
                .isInstanceOfAny(EntityNotFoundException.class, JpaObjectRetrievalFailureException.class);
    }

    private Category category(String name, boolean ownerPublic) {
        Category category = new Category();
        category.setName(name);
        category.setOwnerPublic(ownerPublic);
        return category;
    }
}

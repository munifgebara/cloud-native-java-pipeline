package br.com.stella.api.bdd;

import br.com.munif.common.owner.OwnerContext;
import br.com.munif.common.owner.OwnerIdentity;
import br.com.stella.api.dto.CategoryCreateDTO;
import br.com.stella.api.dto.CategoryResponseDTO;
import br.com.stella.api.entity.Category;
import br.com.stella.api.repository.CategoryRepository;
import br.com.stella.api.service.CategoryService;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategorySteps {

    private static final OwnerIdentity OWNER_A = new OwnerIdentity("owner-a@example.local", "https://issuer.example/realms/stella");
    private static final OwnerIdentity OWNER_B = new OwnerIdentity("owner-b@example.local", "https://issuer.example/realms/stella");

    private CategoryRepository repository;
    private CategoryService service;
    private List<Category> categories;
    private CategoryCreateDTO request;
    private CategoryResponseDTO response;
    private List<String> visibleNames;

    @Before
    public void setUp() {
        OwnerContext.clear();
        categories = new ArrayList<>();
        repository = mock(CategoryRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        service = new CategoryService(repository, entityManager);

        when(repository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(UUID.randomUUID());
            OwnerContext.current().ifPresent(owner -> {
                category.setOwnerEmail(owner.email());
                category.setOwnerIssuer(owner.issuer());
            });
            categories.add(category);
            return category;
        });
        when(repository.findAllActive(any(Sort.class))).thenAnswer(invocation -> categories.stream()
                .filter(Category::isActive)
                .filter(this::isVisibleToCurrentOwner)
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .toList());
    }

    @Given("{string} is using Stella")
    public void ownerIsUsingStella(String ownerName) {
        OwnerContext.set(owner(ownerName));
    }

    @Given("a {word} category registration exists with name {string} and icon {string}")
    public void categoryRegistrationExistsWithNameAndIcon(String visibility, String name, String icon) {
        request = new CategoryCreateDTO(name, null, icon, true, "public".equals(visibility));
    }

    @When("the category is saved")
    public void categoryIsSaved() {
        response = service.create(request);
    }

    @Then("the registered category must be named {string}")
    public void registeredCategoryShouldBeNamed(String name) {
        assertThat(response.name()).isEqualTo(name);
    }

    @Then("the icon of the registered category must be {string}")
    public void registeredCategoryIconShouldBe(String icon) {
        assertThat(response.icon()).isEqualTo(icon);
    }

    @Then("the registered category must be active")
    public void registeredCategoryShouldBeActive() {
        assertThat(response.active()).isTrue();
    }

    @Then("the category must belong to {string}")
    public void categoryShouldBelongToOwner(String ownerName) {
        Category category = categories.getLast();

        assertThat(category.getOwnerEmail()).isEqualTo(owner(ownerName).email());
        assertThat(category.getOwnerIssuer()).isEqualTo(owner(ownerName).issuer());
    }

    @Then("the category must be readable by other owners")
    public void categoryShouldBeReadableByOtherOwners() {
        assertThat(categories.getLast().isOwnerPublic()).isTrue();
    }

    @Then("the category must stay private to its owner")
    public void categoryShouldStayPrivateToItsOwner() {
        assertThat(categories.getLast().isOwnerPublic()).isFalse();
    }

    @When("{string} lists active categories")
    public void ownerListsActiveCategories(String ownerName) {
        OwnerContext.set(owner(ownerName));
        visibleNames = service.listSummary().stream()
                .map(summary -> summary.name())
                .toList();
    }

    @Then("{string} must be visible")
    public void categoryShouldBeVisible(String categoryName) {
        assertThat(visibleNames).contains(categoryName);
    }

    @Then("{string} must not be visible")
    public void categoryShouldNotBeVisible(String categoryName) {
        assertThat(visibleNames).doesNotContain(categoryName);
    }

    private boolean isVisibleToCurrentOwner(Category category) {
        return OwnerContext.current()
                .map(owner -> category.isOwnerPublic()
                        || owner.email().equals(category.getOwnerEmail())
                        && owner.issuer().equals(category.getOwnerIssuer()))
                .orElse(true);
    }

    private OwnerIdentity owner(String ownerName) {
        return switch (ownerName) {
            case "Owner A" -> OWNER_A;
            case "Owner B" -> OWNER_B;
            default -> throw new IllegalArgumentException("Unknown owner: " + ownerName);
        };
    }
}

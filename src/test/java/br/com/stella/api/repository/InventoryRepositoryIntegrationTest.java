package br.com.stella.api.repository;

import br.com.munif.common.persistencia.BaseEntity;
import br.com.stella.api.entity.Category;
import br.com.stella.api.entity.ItemLoan;
import br.com.stella.api.entity.ItemInstance;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.entity.StorageLocation;
import br.com.stella.api.entity.ItemMovement;
import br.com.stella.api.entity.Person;
import br.com.stella.api.entity.ItemInstanceStatus;
import br.com.stella.api.entity.ItemMovementType;
import br.com.stella.api.service.PersonService;
import org.springframework.data.domain.Sort;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InventoryRepositoryIntegrationTest {

    private static final String TEST_OWNER_EMAIL = "integration@example.local";
    private static final String TEST_OWNER_ISSUER = "https://issuer.example.local/realms/test";

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MainItemRepository mainItemRepository;

    @Autowired
    private ItemInstanceRepository itemInstanceRepository;

    @Autowired
    private ItemMovementRepository itemMovementRepository;

    @Autowired
    private ItemLoanRepository itemLoanRepository;

    @Autowired
    private PersonRepository personRepository;

    @Test
    void shouldFilterItemsMainActiveByNameAndCategory() {
        Category ferramentas = category("ITG Ferramentas");
        Category livros = category("ITG Livros");
        persistir(ferramentas, livros);

        MainItem furadeira = mainItem("ITG Furadeira de impacto", ferramentas);
        MainItem livro = mainItem("ITG Livro de arquitetura", livros);
        persistir(furadeira, livro);
        flushAndClear();

        var items = mainItemRepository.findAll(MainItemRepository.filterActive("furadeira", ferramentas.getId()), Sort.by("name"));

        assertThat(items).extracting(MainItem::getName)
                .containsExactly("ITG Furadeira de impacto");
    }

    @Test
    void shouldFilterInstancesActiveByIdentificationItemCategoryAndStatus() {
        Category category = category("ITG Eletronicos");
        StorageLocation location = location("ITG Laboratorio");
        MainItem notebook = mainItem("ITG Notebook Dell", category);
        MainItem projetor = mainItem("ITG Projetor Epson", category);
        persistir(category, location, notebook, projetor);

        ItemInstance instanciaDisponivel = instance(notebook, location, "ITG-NB-001", "ITG-PAT-001", ItemInstanceStatus.DISPONIVEL);
        ItemInstance instanciaEmprestada = instance(projetor, null, "ITG-PRJ-001", "ITG-PAT-002", ItemInstanceStatus.EMPRESTADO);
        persistir(instanciaDisponivel, instanciaEmprestada);
        flushAndClear();

        var instances = itemInstanceRepository.findAll(
                ItemInstanceRepository.filterActive("pat-001", "notebook", category.getId(), ItemInstanceStatus.DISPONIVEL),
                Sort.by("identifier")
        );

        assertThat(instances).extracting(ItemInstance::getIdentifier)
                .containsExactly("ITG-NB-001");
    }

    @Test
    void shouldListLocationsWithMoreItemsByQuantityOfInstancesActive() {
        Category category = category("ITG Organizacao");
        MainItem caixa = mainItem("ITG Caixa organizadora", category);
        StorageLocation deposito = location("ITG Deposito");
        StorageLocation room = location("ITG Sala");
        persistir(category, caixa, deposito, room);

        persistir(
                instance(caixa, deposito, "ITG-DEP-001", null, ItemInstanceStatus.DISPONIVEL),
                instance(caixa, deposito, "ITG-DEP-002", null, ItemInstanceStatus.DISPONIVEL),
                instance(caixa, deposito, "ITG-DEP-003", null, ItemInstanceStatus.DISPONIVEL),
                instance(caixa, room, "ITG-SALA-001", null, ItemInstanceStatus.DISPONIVEL)
        );
        flushAndClear();

        var locais = itemInstanceRepository.findLocationsWithMostItems(PageRequest.of(0, 20)).stream()
                .filter(location -> location.name().startsWith("ITG "))
                .toList();

        assertThat(locais).extracting("name")
                .containsExactly("ITG Deposito", "ITG Sala");
        assertThat(locais).extracting("instanceCount")
                .containsExactly(3L, 1L);
    }

    @Test
    void shouldSortHistoryOfMovementsByData() {
        Category category = category("ITG History");
        MainItem item = mainItem("ITG Patrimonio history", category);
        StorageLocation origem = location("ITG Origem");
        StorageLocation destino = location("ITG Destino");
        persistir(category, item, origem, destino);

        ItemInstance instance = instance(item, destino, "ITG-HIST-001", null, ItemInstanceStatus.DISPONIVEL);
        persistir(instance);

        ItemMovement maisRecente = movement(instance, origem, destino, Instant.parse("2026-01-02T10:00:00Z"));
        ItemMovement maisAntiga = movement(instance, null, origem, Instant.parse("2026-01-01T10:00:00Z"));
        persistir(maisRecente, maisAntiga);
        flushAndClear();

        var history = itemMovementRepository.findByItemInstanceIdOrderByMovementDateAscCreatedAtAsc(instance.getId());

        assertThat(history).extracting(ItemMovement::getMovementDate)
                .containsExactly(
                        Instant.parse("2026-01-01T10:00:00Z"),
                        Instant.parse("2026-01-02T10:00:00Z")
                );
    }

    @Test
    void shouldLocateLoanOpenOfInstance() {
        Category category = category("ITG Emprestimos");
        MainItem item = mainItem("ITG Livro emprestavel", category);
        Person person = person("ITG Maria Silva");
        persistir(category, item, person);

        ItemInstance instance = instance(item, null, "ITG-EMP-001", null, ItemInstanceStatus.EMPRESTADO);
        persistir(instance);

        ItemLoan emprestimoAberto = loan(instance, person, null);
        ItemLoan emprestimoFechado = loan(instance, person, Instant.parse("2026-01-03T10:00:00Z"));
        persistir(emprestimoAberto, emprestimoFechado);
        flushAndClear();

        assertThat(itemLoanRepository.existsByItemInstanceIdAndReturnDateIsNull(instance.getId())).isTrue();
        assertThat(itemLoanRepository.findByItemInstanceIdAndReturnDateIsNull(instance.getId()))
                .get()
                .extracting(ItemLoan::getReturnDate)
                .isNull();
    }

    @Test
    void shouldListRevisionsOfPersonViaEnvers() {
        Person person = person("ITG Ana History");
        persistir(person);
        commitAndStart();

        Person pessoaSalva = entityManager.find(Person.class, person.getId());
        pessoaSalva.setEmail("ana@example.com");
        commitAndStart();

        PersonService service = new PersonService(personRepository, entityManager, null);

        var revisions = service.listRevisions(person.getId());

        assertThat(revisions).hasSize(2);
        assertThat(revisions).extracting("type")
                .containsExactly("MOD", "ADD");
        assertThat(revisions.getFirst().person().email()).isEqualTo("ana@example.com");
        assertThat(revisions.getFirst().changedFields()).containsExactly("email");
        assertThat(revisions.getFirst().timestamp()).isNotNull();
        assertThat(revisions.get(1).changedFields()).isEmpty();
    }

    private Category category(String name) {
        Category category = new Category();
        category.setName(name);
        category.setActive(true);
        return category;
    }

    private MainItem mainItem(String name, Category category) {
        MainItem item = new MainItem();
        item.setName(name);
        item.setCategory(category);
        item.setActive(true);
        return item;
    }

    private StorageLocation location(String name) {
        StorageLocation location = new StorageLocation();
        location.setName(name);
        location.setActive(true);
        return location;
    }

    private ItemInstance instance(
            MainItem item,
            StorageLocation location,
            String identifier,
            String assetTag,
            ItemInstanceStatus status
    ) {
        ItemInstance instance = new ItemInstance();
        instance.setMainItem(item);
        instance.setCurrentLocation(location);
        instance.setIdentifier(identifier);
        instance.setAssetTag(assetTag);
        instance.setOperationalStatus(status);
        instance.setActive(true);
        return instance;
    }

    private ItemMovement movement(
            ItemInstance instance,
            StorageLocation origem,
            StorageLocation destino,
            Instant movementDate
    ) {
        ItemMovement movement = new ItemMovement();
        movement.setType(origem == null ? ItemMovementType.ENTRADA : ItemMovementType.TRANSFERENCIA);
        movement.setItemInstance(instance);
        movement.setOriginLocation(origem);
        movement.setDestinationLocation(destino);
        movement.setMovementDate(movementDate);
        movement.setActive(true);
        return movement;
    }

    private Person person(String name) {
        Person person = new Person();
        person.setName(name);
        person.setTaxId(gerarCpfCnpj());
        person.setActive(true);
        return person;
    }

    private ItemLoan loan(ItemInstance instance, Person person, Instant returnDate) {
        ItemLoan loan = new ItemLoan();
        loan.setItemInstance(instance);
        loan.setPerson(person);
        loan.setExpectedReturnDate(LocalDate.of(2026, 1, 10));
        loan.setReturnDate(returnDate);
        loan.setActive(true);
        return loan;
    }

    private void persistir(Object... entidades) {
        for (Object entity : entidades) {
            if (entity instanceof BaseEntity baseEntity) {
                baseEntity.setOwnerEmail(TEST_OWNER_EMAIL);
                baseEntity.setOwnerIssuer(TEST_OWNER_ISSUER);
            }
            entityManager.persist(entity);
        }
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void commitAndStart() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
    }

    private String gerarCpfCnpj() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 11);
    }
}

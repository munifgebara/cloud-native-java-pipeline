package br.com.stella.api.mapper;

import br.com.stella.api.dto.CategoryCreateDTO;
import br.com.stella.api.dto.CategoryUpdateDTO;
import br.com.stella.api.dto.ItemInstanceCreateDTO;
import br.com.stella.api.dto.ItemInstanceUpdateDTO;
import br.com.stella.api.dto.MainItemCreateDTO;
import br.com.stella.api.dto.MainItemUpdateDTO;
import br.com.stella.api.dto.StorageLocationCreateDTO;
import br.com.stella.api.dto.StorageLocationUpdateDTO;
import br.com.stella.api.dto.PersonCreateDTO;
import br.com.stella.api.dto.PersonUpdateDTO;
import br.com.stella.api.entity.Category;
import br.com.stella.api.entity.ItemLoan;
import br.com.stella.api.entity.ItemInstance;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.entity.StorageLocation;
import br.com.stella.api.entity.ItemMovement;
import br.com.stella.api.entity.Person;
import br.com.stella.api.entity.ItemInstanceStatus;
import br.com.stella.api.entity.ItemMovementType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryMapperTest {

    @Test
    void shouldMapPersonInAllOsContracts() {
        assertThat(PersonMapper.toEntity(null)).isNull();
        assertThat(PersonMapper.toResponseDTO(null)).isNull();
        assertThat(PersonMapper.toResumoDTO(null)).isNull();

        Person person = PersonMapper.toEntity(new PersonCreateDTO(
                "Maria", "12345678901", "1111", "2222", "maria@example.location",
                "01001000", "Rua A", "Ap 1", "Centro", "Sao Paulo", "SP"
        ));

        assertThat(person.getName()).isEqualTo("Maria");
        assertThat(person.getTaxId()).isEqualTo("12345678901");
        assertThat(person.getPrimaryPhone()).isEqualTo("1111");
        assertThat(person.getState()).isEqualTo("SP");

        PersonMapper.updateEntity(person, new PersonUpdateDTO(
                "Maria Silva", "3333", "4444", "maria.silva@example.location",
                "02002000", "Rua B", "Casa", "Jardins", "Campinas", "SP"
        ));

        person.setId(UUID.randomUUID());

        var response = PersonMapper.toResponseDTO(person);
        var resumo = PersonMapper.toResumoDTO(person);

        assertThat(response.name()).isEqualTo("Maria Silva");
        assertThat(response.email()).isEqualTo("maria.silva@example.location");
        assertThat(response.address()).isEqualTo("Rua B");
        assertThat(resumo.id()).isEqualTo(person.getId());
        assertThat(resumo.name()).isEqualTo("Maria Silva");
    }

    @Test
    void shouldIgnoreUpdateOfPersonWhenInboundForNull() {
        Person person = new Person();
        person.setName("Original");

        PersonMapper.updateEntity(null, new PersonUpdateDTO("Novo", null, null, null, null, null, null, null, null, null));
        PersonMapper.updateEntity(person, null);

        assertThat(person.getName()).isEqualTo("Original");
    }

    @Test
    void shouldMapCategoryWithActiveOptional() {
        assertThat(CategoryMapper.toEntity(null)).isNull();
        assertThat(CategoryMapper.toResponseDTO(null)).isNull();
        assertThat(CategoryMapper.toResumoDTO(null)).isNull();

        Category category = CategoryMapper.toEntity(new CategoryCreateDTO("Livros", "Acervo", "livros", false));
        category.setId(UUID.randomUUID());

        assertThat(category.isActive()).isFalse();

        CategoryMapper.updateEntity(category, new CategoryUpdateDTO("Biblioteca", "Livros fisicos", "book", true));

        var response = CategoryMapper.toResponseDTO(category);
        var resumo = CategoryMapper.toResumoDTO(category);

        assertThat(response.name()).isEqualTo("Biblioteca");
        assertThat(response.icon()).isEqualTo("book");
        assertThat(response.active()).isTrue();
        assertThat(resumo.description()).isEqualTo("Livros fisicos");
    }

    @Test
    void shouldPreserveActiveOnMapCategoryWhenFieldVierNull() {
        Category category = CategoryMapper.toEntity(new CategoryCreateDTO("Livros", null, null, null));
        category.setActive(false);

        CategoryMapper.updateEntity(category, new CategoryUpdateDTO("Livros", "Atualizada", null, null));

        assertThat(category.isActive()).isFalse();
        assertThat(category.getDescription()).isEqualTo("Atualizada");
    }

    @Test
    void shouldMapLocationWithPathLevelAndImage() {
        assertThat(StorageLocationMapper.toEntity(null)).isNull();
        assertThat(StorageLocationMapper.toResponseDTO(null)).isNull();
        assertThat(StorageLocationMapper.toResumoDTO(null, "x", 0)).isNull();

        StorageLocation root = location("Casa", null);
        StorageLocation room = location("Sala", root);
        room.setImageObjectKey("locais/%s/photo.png".formatted(room.getId()));
        room.setImageContentType("image/png");
        room.setImageSizeBytes(20L);

        var response = StorageLocationMapper.toResponseDTO(room);
        var resumo = StorageLocationMapper.toResumoDTO(room, "Casa > Sala", 1);

        assertThat(response.parentId()).isEqualTo(root.getId());
        assertThat(response.parentName()).isEqualTo("Casa");
        assertThat(response.path()).isEqualTo("Casa > Sala");
        assertThat(response.level()).isEqualTo(1);
        assertThat(response.imageUrl()).isEqualTo("/api/public/locations/%s/image".formatted(room.getId()));
        assertThat(response.imageContentType()).isEqualTo("image/png");
        assertThat(response.imageSizeBytes()).isEqualTo(20L);
        assertThat(resumo.path()).isEqualTo("Casa > Sala");
        assertThat(resumo.imageUrl()).isEqualTo(response.imageUrl());
    }

    @Test
    void shouldMapCreationAndUpdateOfLocation() {
        StorageLocation location = StorageLocationMapper.toEntity(new StorageLocationCreateDTO("Deposito", "Caixas", null, false));

        assertThat(location.getName()).isEqualTo("Deposito");
        assertThat(location.getDescription()).isEqualTo("Caixas");
        assertThat(location.isActive()).isFalse();

        StorageLocationMapper.updateEntity(location, new StorageLocationUpdateDTO("Almoxarifado", "Materiais", null, true));

        assertThat(location.getName()).isEqualTo("Almoxarifado");
        assertThat(location.getDescription()).isEqualTo("Materiais");
        assertThat(location.isActive()).isTrue();
    }

    @Test
    void shouldMapItemMainWithCategoryAndImage() {
        assertThat(MainItemMapper.toEntity(null)).isNull();
        assertThat(MainItemMapper.toResponseDTO(null)).isNull();
        assertThat(MainItemMapper.toResumoDTO(null)).isNull();

        Category category = category("Ferramentas", "tools");
        MainItem item = MainItemMapper.toEntity(new MainItemCreateDTO("Furadeira", "Impacto", "220V", "CADASTRO_IA_FOTO", category.getId(), false));
        item.setId(UUID.randomUUID());
        item.setCategory(category);
        item.setImageObjectKey("items/%s/photo.png".formatted(item.getId()));
        item.setImageContentType("image/png");
        item.setImageSizeBytes(30L);

        var response = MainItemMapper.toResponseDTO(item);
        var resumo = MainItemMapper.toResumoDTO(item);

        assertThat(response.categoryId()).isEqualTo(category.getId());
        assertThat(response.categoryName()).isEqualTo("Ferramentas");
        assertThat(response.categoryIcon()).isEqualTo("tools");
        assertThat(response.imageUrl()).isEqualTo("/api/public/main-items/%s/main-image".formatted(item.getId()));
        assertThat(response.imageContentType()).isEqualTo("image/png");
        assertThat(response.imageSizeBytes()).isEqualTo(30L);
        assertThat(resumo.imageUrl()).isEqualTo(response.imageUrl());
    }

    @Test
    void shouldUpdateItemMainPreservingActiveWhenFieldVierNull() {
        MainItem item = new MainItem();
        item.setActive(false);

        MainItemMapper.updateEntity(item, new MainItemUpdateDTO("Notebook", "Descricao", "Obs", null, null, null));

        assertThat(item.getName()).isEqualTo("Notebook");
        assertThat(item.getDescription()).isEqualTo("Descricao");
        assertThat(item.getNotes()).isEqualTo("Obs");
        assertThat(item.isActive()).isFalse();
    }

    @Test
    void shouldMapInstanceItemWithDefaultsAndRelationships() {
        assertThat(ItemInstanceMapper.toEntity(null)).isNull();
        assertThat(ItemInstanceMapper.toResponseDTO(null)).isNull();
        assertThat(ItemInstanceMapper.toResumoDTO(null)).isNull();

        Category category = category("Livros", "book");
        MainItem item = item("Clean Code", category);
        StorageLocation location = location("Estante", null);

        ItemInstance instance = ItemInstanceMapper.toEntity(new ItemInstanceCreateDTO(
                item.getId(), location.getId(), "EX-1", "PAT-1", "SER-1", null, "Novo", null, false
        ));
        instance.setId(UUID.randomUUID());
        instance.setMainItem(item);
        instance.setCurrentLocation(location);

        var response = ItemInstanceMapper.toResponseDTO(instance);
        var resumo = ItemInstanceMapper.toResumoDTO(instance);

        assertThat(instance.getOperationalStatus()).isEqualTo(ItemInstanceStatus.DISPONIVEL);
        assertThat(instance.isActive()).isFalse();
        assertThat(response.mainItemName()).isEqualTo("Clean Code");
        assertThat(response.categoryName()).isEqualTo("Livros");
        assertThat(response.currentLocationName()).isEqualTo("Estante");
        assertThat(resumo.categoryIcon()).isEqualTo("book");
    }

    @Test
    void shouldUpdateInstanceWithStatusProvided() {
        ItemInstance instance = new ItemInstance();

        ItemInstanceMapper.updateEntity(instance, new ItemInstanceUpdateDTO(
                UUID.randomUUID(), null, "EX-2", "PAT-2", "SER-2",
                ItemInstanceStatus.EMPRESTADO, "Emprestado", null, true
        ));

        assertThat(instance.getIdentifier()).isEqualTo("EX-2");
        assertThat(instance.getOperationalStatus()).isEqualTo(ItemInstanceStatus.EMPRESTADO);
        assertThat(instance.isActive()).isTrue();
    }

    @Test
    void shouldMapLoanWithFallbackOfIdentification() {
        assertThat(ItemLoanMapper.toResponseDTO(null)).isNull();

        ItemInstance instance = new ItemInstance();
        instance.setId(UUID.randomUUID());
        instance.setAssetTag("PAT-10");
        Person person = new Person();
        person.setId(UUID.randomUUID());
        person.setName("Joao");
        ItemLoan loan = new ItemLoan();
        loan.setId(UUID.randomUUID());
        loan.setItemInstance(instance);
        loan.setPerson(person);
        loan.setLoanDate(Instant.parse("2026-01-01T10:00:00Z"));
        loan.setExpectedReturnDate(LocalDate.parse("2026-01-10"));
        loan.setReturnDate(Instant.parse("2026-01-05T10:00:00Z"));
        loan.setNotes("ok");

        var response = ItemLoanMapper.toResponseDTO(loan);

        assertThat(response.instanceIdentification()).isEqualTo("PAT-10");
        assertThat(response.personName()).isEqualTo("Joao");
        assertThat(response.notes()).isEqualTo("ok");
    }

    @Test
    void shouldMapMovementWithFallbackOfIdentification() {
        assertThat(ItemMovementMapper.toResponseDTO(null)).isNull();

        ItemInstance instance = new ItemInstance();
        instance.setId(UUID.randomUUID());
        instance.setSerialNumber("SER-10");
        StorageLocation origem = location("Origem", null);
        StorageLocation destino = location("Destino", null);
        ItemMovement movement = new ItemMovement();
        movement.setId(UUID.randomUUID());
        movement.setType(ItemMovementType.TRANSFERENCIA);
        movement.setMovementDate(Instant.parse("2026-01-01T10:00:00Z"));
        movement.setItemInstance(instance);
        movement.setOriginLocation(origem);
        movement.setDestinationLocation(destino);
        movement.setReason("Organizacao");
        movement.setNotes("ok");

        var response = ItemMovementMapper.toResponseDTO(movement);

        assertThat(response.instanceIdentification()).isEqualTo("SER-10");
        assertThat(response.originLocationName()).isEqualTo("Origem");
        assertThat(response.destinationLocationName()).isEqualTo("Destino");
        assertThat(response.reason()).isEqualTo("Organizacao");
    }

    private Category category(String name, String icon) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName(name);
        category.setIcon(icon);
        category.setActive(true);
        return category;
    }

    private MainItem item(String name, Category category) {
        MainItem item = new MainItem();
        item.setId(UUID.randomUUID());
        item.setName(name);
        item.setCategory(category);
        item.setActive(true);
        return item;
    }

    private StorageLocation location(String name, StorageLocation parent) {
        StorageLocation location = new StorageLocation();
        location.setId(UUID.randomUUID());
        location.setName(name);
        location.setParent(parent);
        location.setActive(true);
        return location;
    }
}

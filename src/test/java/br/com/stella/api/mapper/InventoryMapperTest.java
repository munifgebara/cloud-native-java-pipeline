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
    void deveMapearPessoaEmTodosOsContratos() {
        assertThat(PersonMapper.toEntity(null)).isNull();
        assertThat(PersonMapper.toResponseDTO(null)).isNull();
        assertThat(PersonMapper.toResumoDTO(null)).isNull();

        Person pessoa = PersonMapper.toEntity(new PersonCreateDTO(
                "Maria", "12345678901", "1111", "2222", "maria@example.location",
                "01001000", "Rua A", "Ap 1", "Centro", "Sao Paulo", "SP"
        ));

        assertThat(pessoa.getName()).isEqualTo("Maria");
        assertThat(pessoa.getTaxId()).isEqualTo("12345678901");
        assertThat(pessoa.getPrimaryPhone()).isEqualTo("1111");
        assertThat(pessoa.getState()).isEqualTo("SP");

        PersonMapper.updateEntity(pessoa, new PersonUpdateDTO(
                "Maria Silva", "3333", "4444", "maria.silva@example.location",
                "02002000", "Rua B", "Casa", "Jardins", "Campinas", "SP"
        ));

        pessoa.setId(UUID.randomUUID());

        var response = PersonMapper.toResponseDTO(pessoa);
        var resumo = PersonMapper.toResumoDTO(pessoa);

        assertThat(response.nome()).isEqualTo("Maria Silva");
        assertThat(response.email()).isEqualTo("maria.silva@example.location");
        assertThat(response.endereco()).isEqualTo("Rua B");
        assertThat(resumo.id()).isEqualTo(pessoa.getId());
        assertThat(resumo.nome()).isEqualTo("Maria Silva");
    }

    @Test
    void deveIgnorarUpdateDePessoaQuandoEntradaForNula() {
        Person pessoa = new Person();
        pessoa.setName("Original");

        PersonMapper.updateEntity(null, new PersonUpdateDTO("Novo", null, null, null, null, null, null, null, null, null));
        PersonMapper.updateEntity(pessoa, null);

        assertThat(pessoa.getName()).isEqualTo("Original");
    }

    @Test
    void deveMapearCategoriaComAtivoOpcional() {
        assertThat(CategoryMapper.toEntity(null)).isNull();
        assertThat(CategoryMapper.toResponseDTO(null)).isNull();
        assertThat(CategoryMapper.toResumoDTO(null)).isNull();

        Category category = CategoryMapper.toEntity(new CategoryCreateDTO("Livros", "Acervo", "livros", false));
        category.setId(UUID.randomUUID());

        assertThat(category.isActive()).isFalse();

        CategoryMapper.updateEntity(category, new CategoryUpdateDTO("Biblioteca", "Livros fisicos", "book", true));

        var response = CategoryMapper.toResponseDTO(category);
        var resumo = CategoryMapper.toResumoDTO(category);

        assertThat(response.nome()).isEqualTo("Biblioteca");
        assertThat(response.icone()).isEqualTo("book");
        assertThat(response.ativa()).isTrue();
        assertThat(resumo.descricao()).isEqualTo("Livros fisicos");
    }

    @Test
    void devePreservarAtivoAoMapearCategoriaQuandoCampoVierNulo() {
        Category category = CategoryMapper.toEntity(new CategoryCreateDTO("Livros", null, null, null));
        category.setActive(false);

        CategoryMapper.updateEntity(category, new CategoryUpdateDTO("Livros", "Atualizada", null, null));

        assertThat(category.isActive()).isFalse();
        assertThat(category.getDescription()).isEqualTo("Atualizada");
    }

    @Test
    void deveMapearLocalComCaminhoNivelEImagem() {
        assertThat(StorageLocationMapper.toEntity(null)).isNull();
        assertThat(StorageLocationMapper.toResponseDTO(null)).isNull();
        assertThat(StorageLocationMapper.toResumoDTO(null, "x", 0)).isNull();

        StorageLocation raiz = location("Casa", null);
        StorageLocation sala = location("Sala", raiz);
        sala.setImageObjectKey("locais/%s/foto.png".formatted(sala.getId()));
        sala.setImageContentType("image/png");
        sala.setImageSizeBytes(20L);

        var response = StorageLocationMapper.toResponseDTO(sala);
        var resumo = StorageLocationMapper.toResumoDTO(sala, "Casa > Sala", 1);

        assertThat(response.paiId()).isEqualTo(raiz.getId());
        assertThat(response.paiNome()).isEqualTo("Casa");
        assertThat(response.caminho()).isEqualTo("Casa > Sala");
        assertThat(response.nivel()).isEqualTo(1);
        assertThat(response.imagemUrl()).isEqualTo("/api/public/locais/%s/imagem".formatted(sala.getId()));
        assertThat(response.imageContentType()).isEqualTo("image/png");
        assertThat(response.imageSizeBytes()).isEqualTo(20L);
        assertThat(resumo.caminho()).isEqualTo("Casa > Sala");
        assertThat(resumo.imagemUrl()).isEqualTo(response.imagemUrl());
    }

    @Test
    void deveMapearCriacaoEAtualizacaoDeLocal() {
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
    void deveMapearItemMestreComCategoriaEImagem() {
        assertThat(MainItemMapper.toEntity(null)).isNull();
        assertThat(MainItemMapper.toResponseDTO(null)).isNull();
        assertThat(MainItemMapper.toResumoDTO(null)).isNull();

        Category category = category("Ferramentas", "tools");
        MainItem item = MainItemMapper.toEntity(new MainItemCreateDTO("Furadeira", "Impacto", "220V", "CADASTRO_IA_FOTO", category.getId(), false));
        item.setId(UUID.randomUUID());
        item.setCategory(category);
        item.setImageObjectKey("itens/%s/foto.png".formatted(item.getId()));
        item.setImageContentType("image/png");
        item.setImageSizeBytes(30L);

        var response = MainItemMapper.toResponseDTO(item);
        var resumo = MainItemMapper.toResumoDTO(item);

        assertThat(response.categoriaId()).isEqualTo(category.getId());
        assertThat(response.categoriaNome()).isEqualTo("Ferramentas");
        assertThat(response.categoriaIcone()).isEqualTo("tools");
        assertThat(response.imagemUrl()).isEqualTo("/api/public/itens-mestre/%s/imagem-principal".formatted(item.getId()));
        assertThat(response.imageContentType()).isEqualTo("image/png");
        assertThat(response.imageSizeBytes()).isEqualTo(30L);
        assertThat(resumo.imagemUrl()).isEqualTo(response.imagemUrl());
    }

    @Test
    void deveAtualizarItemMestrePreservandoAtivoQuandoCampoVierNulo() {
        MainItem item = new MainItem();
        item.setActive(false);

        MainItemMapper.updateEntity(item, new MainItemUpdateDTO("Notebook", "Descricao", "Obs", null, null, null));

        assertThat(item.getName()).isEqualTo("Notebook");
        assertThat(item.getDescription()).isEqualTo("Descricao");
        assertThat(item.getNotes()).isEqualTo("Obs");
        assertThat(item.isActive()).isFalse();
    }

    @Test
    void deveMapearInstanciaItemComDefaultsERelacionamentos() {
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
        assertThat(response.itemMestreNome()).isEqualTo("Clean Code");
        assertThat(response.categoriaNome()).isEqualTo("Livros");
        assertThat(response.localAtualNome()).isEqualTo("Estante");
        assertThat(resumo.categoriaIcone()).isEqualTo("book");
    }

    @Test
    void deveAtualizarInstanciaComStatusInformado() {
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
    void deveMapearEmprestimoComFallbackDeIdentificacao() {
        assertThat(ItemLoanMapper.toResponseDTO(null)).isNull();

        ItemInstance instance = new ItemInstance();
        instance.setId(UUID.randomUUID());
        instance.setAssetTag("PAT-10");
        Person pessoa = new Person();
        pessoa.setId(UUID.randomUUID());
        pessoa.setName("Joao");
        ItemLoan emprestimo = new ItemLoan();
        emprestimo.setId(UUID.randomUUID());
        emprestimo.setItemInstance(instance);
        emprestimo.setPerson(pessoa);
        emprestimo.setLoanDate(Instant.parse("2026-01-01T10:00:00Z"));
        emprestimo.setExpectedReturnDate(LocalDate.parse("2026-01-10"));
        emprestimo.setReturnDate(Instant.parse("2026-01-05T10:00:00Z"));
        emprestimo.setNotes("ok");

        var response = ItemLoanMapper.toResponseDTO(emprestimo);

        assertThat(response.instanciaIdentificacao()).isEqualTo("PAT-10");
        assertThat(response.pessoaNome()).isEqualTo("Joao");
        assertThat(response.observacao()).isEqualTo("ok");
    }

    @Test
    void deveMapearMovimentacaoComFallbackDeIdentificacao() {
        assertThat(ItemMovementMapper.toResponseDTO(null)).isNull();

        ItemInstance instance = new ItemInstance();
        instance.setId(UUID.randomUUID());
        instance.setSerialNumber("SER-10");
        StorageLocation origem = location("Origem", null);
        StorageLocation destino = location("Destino", null);
        ItemMovement movimentacao = new ItemMovement();
        movimentacao.setId(UUID.randomUUID());
        movimentacao.setType(ItemMovementType.TRANSFERENCIA);
        movimentacao.setMovementDate(Instant.parse("2026-01-01T10:00:00Z"));
        movimentacao.setItemInstance(instance);
        movimentacao.setOriginLocation(origem);
        movimentacao.setDestinationLocation(destino);
        movimentacao.setReason("Organizacao");
        movimentacao.setNotes("ok");

        var response = ItemMovementMapper.toResponseDTO(movimentacao);

        assertThat(response.instanciaIdentificacao()).isEqualTo("SER-10");
        assertThat(response.localOrigemNome()).isEqualTo("Origem");
        assertThat(response.localDestinoNome()).isEqualTo("Destino");
        assertThat(response.motivo()).isEqualTo("Organizacao");
    }

    private Category category(String nome, String icone) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName(nome);
        category.setIcon(icone);
        category.setActive(true);
        return category;
    }

    private MainItem item(String nome, Category category) {
        MainItem item = new MainItem();
        item.setId(UUID.randomUUID());
        item.setName(nome);
        item.setCategory(category);
        item.setActive(true);
        return item;
    }

    private StorageLocation location(String nome, StorageLocation pai) {
        StorageLocation location = new StorageLocation();
        location.setId(UUID.randomUUID());
        location.setName(nome);
        location.setParent(pai);
        location.setActive(true);
        return location;
    }
}

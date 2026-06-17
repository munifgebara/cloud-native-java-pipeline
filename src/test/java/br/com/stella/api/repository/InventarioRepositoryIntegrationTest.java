package br.com.stella.api.repository;

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
class InventarioRepositoryIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MainItemRepository itemMestreRepository;

    @Autowired
    private ItemInstanceRepository instanciaItemRepository;

    @Autowired
    private ItemMovementRepository movimentacaoItemRepository;

    @Autowired
    private ItemLoanRepository emprestimoItemRepository;

    @Autowired
    private PersonRepository pessoaRepository;

    @Test
    void deveFiltrarItensMestreAtivosPorNomeECategoria() {
        Category ferramentas = category("ITG Ferramentas");
        Category livros = category("ITG Livros");
        persistir(ferramentas, livros);

        MainItem furadeira = mainItem("ITG Furadeira de impacto", ferramentas);
        MainItem livro = mainItem("ITG Livro de arquitetura", livros);
        persistir(furadeira, livro);
        flushAndClear();

        var itens = itemMestreRepository.findAll(MainItemRepository.filtrarAtivos("furadeira", ferramentas.getId()), Sort.by("name"));

        assertThat(itens).extracting(MainItem::getName)
                .containsExactly("ITG Furadeira de impacto");
    }

    @Test
    void deveFiltrarInstanciasAtivasPorIdentificacaoItemCategoriaEStatus() {
        Category category = category("ITG Eletronicos");
        StorageLocation location = location("ITG Laboratorio");
        MainItem notebook = mainItem("ITG Notebook Dell", category);
        MainItem projetor = mainItem("ITG Projetor Epson", category);
        persistir(category, location, notebook, projetor);

        ItemInstance instanciaDisponivel = instance(notebook, location, "ITG-NB-001", "ITG-PAT-001", ItemInstanceStatus.DISPONIVEL);
        ItemInstance instanciaEmprestada = instance(projetor, null, "ITG-PRJ-001", "ITG-PAT-002", ItemInstanceStatus.EMPRESTADO);
        persistir(instanciaDisponivel, instanciaEmprestada);
        flushAndClear();

        var instancias = instanciaItemRepository.findAll(
                ItemInstanceRepository.filtrarAtivas("pat-001", "notebook", category.getId(), ItemInstanceStatus.DISPONIVEL),
                Sort.by("identifier")
        );

        assertThat(instancias).extracting(ItemInstance::getIdentifier)
                .containsExactly("ITG-NB-001");
    }

    @Test
    void deveListarLocaisComMaisItensPelaQuantidadeDeInstanciasAtivas() {
        Category category = category("ITG Organizacao");
        MainItem caixa = mainItem("ITG Caixa organizadora", category);
        StorageLocation deposito = location("ITG Deposito");
        StorageLocation sala = location("ITG Sala");
        persistir(category, caixa, deposito, sala);

        persistir(
                instance(caixa, deposito, "ITG-DEP-001", null, ItemInstanceStatus.DISPONIVEL),
                instance(caixa, deposito, "ITG-DEP-002", null, ItemInstanceStatus.DISPONIVEL),
                instance(caixa, deposito, "ITG-DEP-003", null, ItemInstanceStatus.DISPONIVEL),
                instance(caixa, sala, "ITG-SALA-001", null, ItemInstanceStatus.DISPONIVEL)
        );
        flushAndClear();

        var locais = instanciaItemRepository.buscarLocaisComMaisItens(PageRequest.of(0, 20)).stream()
                .filter(location -> location.nome().startsWith("ITG "))
                .toList();

        assertThat(locais).extracting("nome")
                .containsExactly("ITG Deposito", "ITG Sala");
        assertThat(locais).extracting("quantidadeInstancias")
                .containsExactly(3L, 1L);
    }

    @Test
    void deveOrdenarHistoricoDeMovimentacoesPorData() {
        Category category = category("ITG History");
        MainItem item = mainItem("ITG Patrimonio historico", category);
        StorageLocation origem = location("ITG Origem");
        StorageLocation destino = location("ITG Destino");
        persistir(category, item, origem, destino);

        ItemInstance instance = instance(item, destino, "ITG-HIST-001", null, ItemInstanceStatus.DISPONIVEL);
        persistir(instance);

        ItemMovement maisRecente = movimentacao(instance, origem, destino, Instant.parse("2026-01-02T10:00:00Z"));
        ItemMovement maisAntiga = movimentacao(instance, null, origem, Instant.parse("2026-01-01T10:00:00Z"));
        persistir(maisRecente, maisAntiga);
        flushAndClear();

        var historico = movimentacaoItemRepository.findByItemInstanceIdOrderByDataMovimentacaoAscCriadoEmAsc(instance.getId());

        assertThat(historico).extracting(ItemMovement::getDataMovimentacao)
                .containsExactly(
                        Instant.parse("2026-01-01T10:00:00Z"),
                        Instant.parse("2026-01-02T10:00:00Z")
                );
    }

    @Test
    void deveLocalizarEmprestimoAbertoDaInstancia() {
        Category category = category("ITG Emprestimos");
        MainItem item = mainItem("ITG Livro emprestavel", category);
        Person pessoa = pessoa("ITG Maria Silva");
        persistir(category, item, pessoa);

        ItemInstance instance = instance(item, null, "ITG-EMP-001", null, ItemInstanceStatus.EMPRESTADO);
        persistir(instance);

        ItemLoan emprestimoAberto = emprestimo(instance, pessoa, null);
        ItemLoan emprestimoFechado = emprestimo(instance, pessoa, Instant.parse("2026-01-03T10:00:00Z"));
        persistir(emprestimoAberto, emprestimoFechado);
        flushAndClear();

        assertThat(emprestimoItemRepository.existsByItemInstanceIdAndReturnDateIsNull(instance.getId())).isTrue();
        assertThat(emprestimoItemRepository.findByItemInstanceIdAndReturnDateIsNull(instance.getId()))
                .get()
                .extracting(ItemLoan::getReturnDate)
                .isNull();
    }

    @Test
    void deveListarRevisoesDePessoaViaEnvers() {
        Person pessoa = pessoa("ITG Ana History");
        persistir(pessoa);
        commitAndStart();

        Person pessoaSalva = entityManager.find(Person.class, pessoa.getId());
        pessoaSalva.setEmail("ana@example.com");
        commitAndStart();

        PersonService service = new PersonService(pessoaRepository, entityManager);

        var revisoes = service.listarRevisoes(pessoa.getId());

        assertThat(revisoes).hasSize(2);
        assertThat(revisoes).extracting("tipo")
                .containsExactly("MOD", "ADD");
        assertThat(revisoes.getFirst().pessoa().email()).isEqualTo("ana@example.com");
        assertThat(revisoes.getFirst().camposAlterados()).containsExactly("email");
        assertThat(revisoes.getFirst().dataHora()).isNotNull();
        assertThat(revisoes.get(1).camposAlterados()).isEmpty();
    }

    private Category category(String nome) {
        Category category = new Category();
        category.setName(nome);
        category.setActive(true);
        return category;
    }

    private MainItem mainItem(String nome, Category category) {
        MainItem item = new MainItem();
        item.setName(nome);
        item.setCategory(category);
        item.setActive(true);
        return item;
    }

    private StorageLocation location(String nome) {
        StorageLocation location = new StorageLocation();
        location.setName(nome);
        location.setActive(true);
        return location;
    }

    private ItemInstance instance(
            MainItem item,
            StorageLocation location,
            String identificador,
            String patrimonio,
            ItemInstanceStatus status
    ) {
        ItemInstance instance = new ItemInstance();
        instance.setMainItem(item);
        instance.setCurrentLocation(location);
        instance.setIdentifier(identificador);
        instance.setAssetTag(patrimonio);
        instance.setOperationalStatus(status);
        instance.setActive(true);
        return instance;
    }

    private ItemMovement movimentacao(
            ItemInstance instance,
            StorageLocation origem,
            StorageLocation destino,
            Instant dataMovimentacao
    ) {
        ItemMovement movimentacao = new ItemMovement();
        movimentacao.setType(origem == null ? ItemMovementType.ENTRADA : ItemMovementType.TRANSFERENCIA);
        movimentacao.setItemInstance(instance);
        movimentacao.setOriginLocation(origem);
        movimentacao.setDestinationLocation(destino);
        movimentacao.setDataMovimentacao(dataMovimentacao);
        movimentacao.setActive(true);
        return movimentacao;
    }

    private Person pessoa(String nome) {
        Person pessoa = new Person();
        pessoa.setName(nome);
        pessoa.setTaxId(gerarCpfCnpj());
        pessoa.setActive(true);
        return pessoa;
    }

    private ItemLoan emprestimo(ItemInstance instance, Person pessoa, Instant dataDevolucao) {
        ItemLoan emprestimo = new ItemLoan();
        emprestimo.setItemInstance(instance);
        emprestimo.setPerson(pessoa);
        emprestimo.setExpectedReturnDate(LocalDate.of(2026, 1, 10));
        emprestimo.setReturnDate(dataDevolucao);
        emprestimo.setActive(true);
        return emprestimo;
    }

    private void persistir(Object... entidades) {
        for (Object entidade : entidades) {
            entityManager.persist(entidade);
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

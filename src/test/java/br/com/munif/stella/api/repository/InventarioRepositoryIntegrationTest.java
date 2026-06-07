package br.com.munif.stella.api.repository;

import br.com.munif.stella.api.entity.Categoria;
import br.com.munif.stella.api.entity.EmprestimoItem;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.ItemMestre;
import br.com.munif.stella.api.entity.LocalArmazenamento;
import br.com.munif.stella.api.entity.MovimentacaoItem;
import br.com.munif.stella.api.entity.Pessoa;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import br.com.munif.stella.api.entity.TipoMovimentacaoItem;
import br.com.munif.stella.api.service.PessoaService;
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
    private ItemMestreRepository itemMestreRepository;

    @Autowired
    private InstanciaItemRepository instanciaItemRepository;

    @Autowired
    private MovimentacaoItemRepository movimentacaoItemRepository;

    @Autowired
    private EmprestimoItemRepository emprestimoItemRepository;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Test
    void deveFiltrarItensMestreAtivosPorNomeECategoria() {
        Categoria ferramentas = categoria("ITG Ferramentas");
        Categoria livros = categoria("ITG Livros");
        persistir(ferramentas, livros);

        ItemMestre furadeira = itemMestre("ITG Furadeira de impacto", ferramentas);
        ItemMestre livro = itemMestre("ITG Livro de arquitetura", livros);
        persistir(furadeira, livro);
        flushAndClear();

        var itens = itemMestreRepository.filtrarAtivos("furadeira", ferramentas.getId());

        assertThat(itens).extracting(ItemMestre::getNome)
                .containsExactly("ITG Furadeira de impacto");
    }

    @Test
    void deveFiltrarInstanciasAtivasPorIdentificacaoItemCategoriaEStatus() {
        Categoria categoria = categoria("ITG Eletronicos");
        LocalArmazenamento local = local("ITG Laboratorio");
        ItemMestre notebook = itemMestre("ITG Notebook Dell", categoria);
        ItemMestre projetor = itemMestre("ITG Projetor Epson", categoria);
        persistir(categoria, local, notebook, projetor);

        InstanciaItem instanciaDisponivel = instancia(notebook, local, "ITG-NB-001", "ITG-PAT-001", StatusOperacionalInstancia.DISPONIVEL);
        InstanciaItem instanciaEmprestada = instancia(projetor, null, "ITG-PRJ-001", "ITG-PAT-002", StatusOperacionalInstancia.EMPRESTADO);
        persistir(instanciaDisponivel, instanciaEmprestada);
        flushAndClear();

        var instancias = instanciaItemRepository.filtrarAtivas(
                "pat-001",
                "notebook",
                categoria.getId(),
                StatusOperacionalInstancia.DISPONIVEL
        );

        assertThat(instancias).extracting(InstanciaItem::getIdentificador)
                .containsExactly("ITG-NB-001");
    }

    @Test
    void deveListarLocaisComMaisItensPelaQuantidadeDeInstanciasAtivas() {
        Categoria categoria = categoria("ITG Organizacao");
        ItemMestre caixa = itemMestre("ITG Caixa organizadora", categoria);
        LocalArmazenamento deposito = local("ITG Deposito");
        LocalArmazenamento sala = local("ITG Sala");
        persistir(categoria, caixa, deposito, sala);

        persistir(
                instancia(caixa, deposito, "ITG-DEP-001", null, StatusOperacionalInstancia.DISPONIVEL),
                instancia(caixa, deposito, "ITG-DEP-002", null, StatusOperacionalInstancia.DISPONIVEL),
                instancia(caixa, deposito, "ITG-DEP-003", null, StatusOperacionalInstancia.DISPONIVEL),
                instancia(caixa, sala, "ITG-SALA-001", null, StatusOperacionalInstancia.DISPONIVEL)
        );
        flushAndClear();

        var locais = instanciaItemRepository.buscarLocaisComMaisItens(PageRequest.of(0, 20)).stream()
                .filter(local -> local.nome().startsWith("ITG "))
                .toList();

        assertThat(locais).extracting("nome")
                .containsExactly("ITG Deposito", "ITG Sala");
        assertThat(locais).extracting("quantidadeInstancias")
                .containsExactly(3L, 1L);
    }

    @Test
    void deveOrdenarHistoricoDeMovimentacoesPorData() {
        Categoria categoria = categoria("ITG Historico");
        ItemMestre item = itemMestre("ITG Patrimonio historico", categoria);
        LocalArmazenamento origem = local("ITG Origem");
        LocalArmazenamento destino = local("ITG Destino");
        persistir(categoria, item, origem, destino);

        InstanciaItem instancia = instancia(item, destino, "ITG-HIST-001", null, StatusOperacionalInstancia.DISPONIVEL);
        persistir(instancia);

        MovimentacaoItem maisRecente = movimentacao(instancia, origem, destino, Instant.parse("2026-01-02T10:00:00Z"));
        MovimentacaoItem maisAntiga = movimentacao(instancia, null, origem, Instant.parse("2026-01-01T10:00:00Z"));
        persistir(maisRecente, maisAntiga);
        flushAndClear();

        var historico = movimentacaoItemRepository.findByInstanciaItemIdOrderByDataMovimentacaoAscCriadoEmAsc(instancia.getId());

        assertThat(historico).extracting(MovimentacaoItem::getDataMovimentacao)
                .containsExactly(
                        Instant.parse("2026-01-01T10:00:00Z"),
                        Instant.parse("2026-01-02T10:00:00Z")
                );
    }

    @Test
    void deveLocalizarEmprestimoAbertoDaInstancia() {
        Categoria categoria = categoria("ITG Emprestimos");
        ItemMestre item = itemMestre("ITG Livro emprestavel", categoria);
        Pessoa pessoa = pessoa("ITG Maria Silva");
        persistir(categoria, item, pessoa);

        InstanciaItem instancia = instancia(item, null, "ITG-EMP-001", null, StatusOperacionalInstancia.EMPRESTADO);
        persistir(instancia);

        EmprestimoItem emprestimoAberto = emprestimo(instancia, pessoa, null);
        EmprestimoItem emprestimoFechado = emprestimo(instancia, pessoa, Instant.parse("2026-01-03T10:00:00Z"));
        persistir(emprestimoAberto, emprestimoFechado);
        flushAndClear();

        assertThat(emprestimoItemRepository.existsByInstanciaItemIdAndDataDevolucaoIsNull(instancia.getId())).isTrue();
        assertThat(emprestimoItemRepository.findByInstanciaItemIdAndDataDevolucaoIsNull(instancia.getId()))
                .get()
                .extracting(EmprestimoItem::getDataDevolucao)
                .isNull();
    }

    @Test
    void deveListarRevisoesDePessoaViaEnvers() {
        Pessoa pessoa = pessoa("ITG Ana Historico");
        persistir(pessoa);
        commitAndStart();

        Pessoa pessoaSalva = entityManager.find(Pessoa.class, pessoa.getId());
        pessoaSalva.setEmail("ana@example.com");
        commitAndStart();

        PessoaService service = new PessoaService(pessoaRepository, entityManager);

        var revisoes = service.listarRevisoes(pessoa.getId());

        assertThat(revisoes).hasSize(2);
        assertThat(revisoes).extracting("tipo")
                .containsExactly("MOD", "ADD");
        assertThat(revisoes.getFirst().pessoa().email()).isEqualTo("ana@example.com");
        assertThat(revisoes.getFirst().dataHora()).isNotNull();
    }

    private Categoria categoria(String nome) {
        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        categoria.setAtivo(true);
        return categoria;
    }

    private ItemMestre itemMestre(String nome, Categoria categoria) {
        ItemMestre item = new ItemMestre();
        item.setNome(nome);
        item.setCategoria(categoria);
        item.setAtivo(true);
        return item;
    }

    private LocalArmazenamento local(String nome) {
        LocalArmazenamento local = new LocalArmazenamento();
        local.setNome(nome);
        local.setAtivo(true);
        return local;
    }

    private InstanciaItem instancia(
            ItemMestre item,
            LocalArmazenamento local,
            String identificador,
            String patrimonio,
            StatusOperacionalInstancia status
    ) {
        InstanciaItem instancia = new InstanciaItem();
        instancia.setItemMestre(item);
        instancia.setLocalAtual(local);
        instancia.setIdentificador(identificador);
        instancia.setPatrimonio(patrimonio);
        instancia.setStatusOperacional(status);
        instancia.setAtivo(true);
        return instancia;
    }

    private MovimentacaoItem movimentacao(
            InstanciaItem instancia,
            LocalArmazenamento origem,
            LocalArmazenamento destino,
            Instant dataMovimentacao
    ) {
        MovimentacaoItem movimentacao = new MovimentacaoItem();
        movimentacao.setTipo(origem == null ? TipoMovimentacaoItem.ENTRADA : TipoMovimentacaoItem.TRANSFERENCIA);
        movimentacao.setInstanciaItem(instancia);
        movimentacao.setLocalOrigem(origem);
        movimentacao.setLocalDestino(destino);
        movimentacao.setDataMovimentacao(dataMovimentacao);
        movimentacao.setAtivo(true);
        return movimentacao;
    }

    private Pessoa pessoa(String nome) {
        Pessoa pessoa = new Pessoa();
        pessoa.setNome(nome);
        pessoa.setCpfCnpj(gerarCpfCnpj());
        pessoa.setAtivo(true);
        return pessoa;
    }

    private EmprestimoItem emprestimo(InstanciaItem instancia, Pessoa pessoa, Instant dataDevolucao) {
        EmprestimoItem emprestimo = new EmprestimoItem();
        emprestimo.setInstanciaItem(instancia);
        emprestimo.setPessoa(pessoa);
        emprestimo.setPrevisaoDevolucao(LocalDate.of(2026, 1, 10));
        emprestimo.setDataDevolucao(dataDevolucao);
        emprestimo.setAtivo(true);
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

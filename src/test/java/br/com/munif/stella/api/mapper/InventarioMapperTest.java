package br.com.munif.stella.api.mapper;

import br.com.munif.stella.api.dto.CategoriaCreateDTO;
import br.com.munif.stella.api.dto.CategoriaUpdateDTO;
import br.com.munif.stella.api.dto.InstanciaItemCreateDTO;
import br.com.munif.stella.api.dto.InstanciaItemUpdateDTO;
import br.com.munif.stella.api.dto.ItemMestreCreateDTO;
import br.com.munif.stella.api.dto.ItemMestreUpdateDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoCreateDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoUpdateDTO;
import br.com.munif.stella.api.dto.PessoaCreateDTO;
import br.com.munif.stella.api.dto.PessoaUpdateDTO;
import br.com.munif.stella.api.entity.Categoria;
import br.com.munif.stella.api.entity.EmprestimoItem;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.ItemMestre;
import br.com.munif.stella.api.entity.LocalArmazenamento;
import br.com.munif.stella.api.entity.MovimentacaoItem;
import br.com.munif.stella.api.entity.Pessoa;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import br.com.munif.stella.api.entity.TipoMovimentacaoItem;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InventarioMapperTest {

    @Test
    void deveMapearPessoaEmTodosOsContratos() {
        assertThat(PessoaMapper.toEntity(null)).isNull();
        assertThat(PessoaMapper.toResponseDTO(null)).isNull();
        assertThat(PessoaMapper.toResumoDTO(null)).isNull();

        Pessoa pessoa = PessoaMapper.toEntity(new PessoaCreateDTO(
                "Maria", "12345678901", "1111", "2222", "maria@example.local",
                "01001000", "Rua A", "Ap 1", "Centro", "Sao Paulo", "SP"
        ));

        assertThat(pessoa.getNome()).isEqualTo("Maria");
        assertThat(pessoa.getCpfCnpj()).isEqualTo("12345678901");
        assertThat(pessoa.getTelefonePrincipal()).isEqualTo("1111");
        assertThat(pessoa.getUf()).isEqualTo("SP");

        PessoaMapper.updateEntity(pessoa, new PessoaUpdateDTO(
                "Maria Silva", "3333", "4444", "maria.silva@example.local",
                "02002000", "Rua B", "Casa", "Jardins", "Campinas", "SP"
        ));

        pessoa.setId(UUID.randomUUID());

        var response = PessoaMapper.toResponseDTO(pessoa);
        var resumo = PessoaMapper.toResumoDTO(pessoa);

        assertThat(response.nome()).isEqualTo("Maria Silva");
        assertThat(response.email()).isEqualTo("maria.silva@example.local");
        assertThat(response.endereco()).isEqualTo("Rua B");
        assertThat(resumo.id()).isEqualTo(pessoa.getId());
        assertThat(resumo.nome()).isEqualTo("Maria Silva");
    }

    @Test
    void deveIgnorarUpdateDePessoaQuandoEntradaForNula() {
        Pessoa pessoa = new Pessoa();
        pessoa.setNome("Original");

        PessoaMapper.updateEntity(null, new PessoaUpdateDTO("Novo", null, null, null, null, null, null, null, null, null));
        PessoaMapper.updateEntity(pessoa, null);

        assertThat(pessoa.getNome()).isEqualTo("Original");
    }

    @Test
    void deveMapearCategoriaComAtivoOpcional() {
        assertThat(CategoriaMapper.toEntity(null)).isNull();
        assertThat(CategoriaMapper.toResponseDTO(null)).isNull();
        assertThat(CategoriaMapper.toResumoDTO(null)).isNull();

        Categoria categoria = CategoriaMapper.toEntity(new CategoriaCreateDTO("Livros", "Acervo", "livros", false));
        categoria.setId(UUID.randomUUID());

        assertThat(categoria.isAtivo()).isFalse();

        CategoriaMapper.updateEntity(categoria, new CategoriaUpdateDTO("Biblioteca", "Livros fisicos", "book", true));

        var response = CategoriaMapper.toResponseDTO(categoria);
        var resumo = CategoriaMapper.toResumoDTO(categoria);

        assertThat(response.nome()).isEqualTo("Biblioteca");
        assertThat(response.icone()).isEqualTo("book");
        assertThat(response.ativa()).isTrue();
        assertThat(resumo.descricao()).isEqualTo("Livros fisicos");
    }

    @Test
    void devePreservarAtivoAoMapearCategoriaQuandoCampoVierNulo() {
        Categoria categoria = CategoriaMapper.toEntity(new CategoriaCreateDTO("Livros", null, null, null));
        categoria.setAtivo(false);

        CategoriaMapper.updateEntity(categoria, new CategoriaUpdateDTO("Livros", "Atualizada", null, null));

        assertThat(categoria.isAtivo()).isFalse();
        assertThat(categoria.getDescricao()).isEqualTo("Atualizada");
    }

    @Test
    void deveMapearLocalComCaminhoNivelEImagem() {
        assertThat(LocalArmazenamentoMapper.toEntity(null)).isNull();
        assertThat(LocalArmazenamentoMapper.toResponseDTO(null)).isNull();
        assertThat(LocalArmazenamentoMapper.toResumoDTO(null, "x", 0)).isNull();

        LocalArmazenamento raiz = local("Casa", null);
        LocalArmazenamento sala = local("Sala", raiz);
        sala.setImagemObjectKey("locais/%s/foto.png".formatted(sala.getId()));
        sala.setImagemContentType("image/png");
        sala.setImagemTamanhoBytes(20L);

        var response = LocalArmazenamentoMapper.toResponseDTO(sala);
        var resumo = LocalArmazenamentoMapper.toResumoDTO(sala, "Casa > Sala", 1);

        assertThat(response.paiId()).isEqualTo(raiz.getId());
        assertThat(response.paiNome()).isEqualTo("Casa");
        assertThat(response.caminho()).isEqualTo("Casa > Sala");
        assertThat(response.nivel()).isEqualTo(1);
        assertThat(response.imagemUrl()).isEqualTo("/api/public/locais/%s/imagem".formatted(sala.getId()));
        assertThat(response.imagemContentType()).isEqualTo("image/png");
        assertThat(response.imagemTamanhoBytes()).isEqualTo(20L);
        assertThat(resumo.caminho()).isEqualTo("Casa > Sala");
        assertThat(resumo.imagemUrl()).isEqualTo(response.imagemUrl());
    }

    @Test
    void deveMapearCriacaoEAtualizacaoDeLocal() {
        LocalArmazenamento local = LocalArmazenamentoMapper.toEntity(new LocalArmazenamentoCreateDTO("Deposito", "Caixas", null, false));

        assertThat(local.getNome()).isEqualTo("Deposito");
        assertThat(local.getDescricao()).isEqualTo("Caixas");
        assertThat(local.isAtivo()).isFalse();

        LocalArmazenamentoMapper.updateEntity(local, new LocalArmazenamentoUpdateDTO("Almoxarifado", "Materiais", null, true));

        assertThat(local.getNome()).isEqualTo("Almoxarifado");
        assertThat(local.getDescricao()).isEqualTo("Materiais");
        assertThat(local.isAtivo()).isTrue();
    }

    @Test
    void deveMapearItemMestreComCategoriaEImagem() {
        assertThat(ItemMestreMapper.toEntity(null)).isNull();
        assertThat(ItemMestreMapper.toResponseDTO(null)).isNull();
        assertThat(ItemMestreMapper.toResumoDTO(null)).isNull();

        Categoria categoria = categoria("Ferramentas", "tools");
        ItemMestre item = ItemMestreMapper.toEntity(new ItemMestreCreateDTO("Furadeira", "Impacto", "220V", categoria.getId(), false));
        item.setId(UUID.randomUUID());
        item.setCategoria(categoria);
        item.setImagemObjectKey("itens/%s/foto.png".formatted(item.getId()));
        item.setImagemContentType("image/png");
        item.setImagemTamanhoBytes(30L);

        var response = ItemMestreMapper.toResponseDTO(item);
        var resumo = ItemMestreMapper.toResumoDTO(item);

        assertThat(response.categoriaId()).isEqualTo(categoria.getId());
        assertThat(response.categoriaNome()).isEqualTo("Ferramentas");
        assertThat(response.categoriaIcone()).isEqualTo("tools");
        assertThat(response.imagemUrl()).isEqualTo("/api/public/itens-mestre/%s/imagem-principal".formatted(item.getId()));
        assertThat(response.imagemContentType()).isEqualTo("image/png");
        assertThat(response.imagemTamanhoBytes()).isEqualTo(30L);
        assertThat(resumo.imagemUrl()).isEqualTo(response.imagemUrl());
    }

    @Test
    void deveAtualizarItemMestrePreservandoAtivoQuandoCampoVierNulo() {
        ItemMestre item = new ItemMestre();
        item.setAtivo(false);

        ItemMestreMapper.updateEntity(item, new ItemMestreUpdateDTO("Notebook", "Descricao", "Obs", null, null));

        assertThat(item.getNome()).isEqualTo("Notebook");
        assertThat(item.getDescricao()).isEqualTo("Descricao");
        assertThat(item.getObservacoes()).isEqualTo("Obs");
        assertThat(item.isAtivo()).isFalse();
    }

    @Test
    void deveMapearInstanciaItemComDefaultsERelacionamentos() {
        assertThat(InstanciaItemMapper.toEntity(null)).isNull();
        assertThat(InstanciaItemMapper.toResponseDTO(null)).isNull();
        assertThat(InstanciaItemMapper.toResumoDTO(null)).isNull();

        Categoria categoria = categoria("Livros", "book");
        ItemMestre item = item("Clean Code", categoria);
        LocalArmazenamento local = local("Estante", null);

        InstanciaItem instancia = InstanciaItemMapper.toEntity(new InstanciaItemCreateDTO(
                item.getId(), local.getId(), "EX-1", "PAT-1", "SER-1", null, "Novo", false
        ));
        instancia.setId(UUID.randomUUID());
        instancia.setItemMestre(item);
        instancia.setLocalAtual(local);

        var response = InstanciaItemMapper.toResponseDTO(instancia);
        var resumo = InstanciaItemMapper.toResumoDTO(instancia);

        assertThat(instancia.getStatusOperacional()).isEqualTo(StatusOperacionalInstancia.DISPONIVEL);
        assertThat(instancia.isAtivo()).isFalse();
        assertThat(response.itemMestreNome()).isEqualTo("Clean Code");
        assertThat(response.categoriaNome()).isEqualTo("Livros");
        assertThat(response.localAtualNome()).isEqualTo("Estante");
        assertThat(resumo.categoriaIcone()).isEqualTo("book");
    }

    @Test
    void deveAtualizarInstanciaComStatusInformado() {
        InstanciaItem instancia = new InstanciaItem();

        InstanciaItemMapper.updateEntity(instancia, new InstanciaItemUpdateDTO(
                UUID.randomUUID(), null, "EX-2", "PAT-2", "SER-2",
                StatusOperacionalInstancia.EMPRESTADO, "Emprestado", true
        ));

        assertThat(instancia.getIdentificador()).isEqualTo("EX-2");
        assertThat(instancia.getStatusOperacional()).isEqualTo(StatusOperacionalInstancia.EMPRESTADO);
        assertThat(instancia.isAtivo()).isTrue();
    }

    @Test
    void deveMapearEmprestimoComFallbackDeIdentificacao() {
        assertThat(EmprestimoItemMapper.toResponseDTO(null)).isNull();

        InstanciaItem instancia = new InstanciaItem();
        instancia.setId(UUID.randomUUID());
        instancia.setPatrimonio("PAT-10");
        Pessoa pessoa = new Pessoa();
        pessoa.setId(UUID.randomUUID());
        pessoa.setNome("Joao");
        EmprestimoItem emprestimo = new EmprestimoItem();
        emprestimo.setId(UUID.randomUUID());
        emprestimo.setInstanciaItem(instancia);
        emprestimo.setPessoa(pessoa);
        emprestimo.setDataEmprestimo(Instant.parse("2026-01-01T10:00:00Z"));
        emprestimo.setPrevisaoDevolucao(LocalDate.parse("2026-01-10"));
        emprestimo.setDataDevolucao(Instant.parse("2026-01-05T10:00:00Z"));
        emprestimo.setObservacao("ok");

        var response = EmprestimoItemMapper.toResponseDTO(emprestimo);

        assertThat(response.instanciaIdentificacao()).isEqualTo("PAT-10");
        assertThat(response.pessoaNome()).isEqualTo("Joao");
        assertThat(response.observacao()).isEqualTo("ok");
    }

    @Test
    void deveMapearMovimentacaoComFallbackDeIdentificacao() {
        assertThat(MovimentacaoItemMapper.toResponseDTO(null)).isNull();

        InstanciaItem instancia = new InstanciaItem();
        instancia.setId(UUID.randomUUID());
        instancia.setNumeroSerie("SER-10");
        LocalArmazenamento origem = local("Origem", null);
        LocalArmazenamento destino = local("Destino", null);
        MovimentacaoItem movimentacao = new MovimentacaoItem();
        movimentacao.setId(UUID.randomUUID());
        movimentacao.setTipo(TipoMovimentacaoItem.TRANSFERENCIA);
        movimentacao.setDataMovimentacao(Instant.parse("2026-01-01T10:00:00Z"));
        movimentacao.setInstanciaItem(instancia);
        movimentacao.setLocalOrigem(origem);
        movimentacao.setLocalDestino(destino);
        movimentacao.setMotivo("Organizacao");
        movimentacao.setObservacao("ok");

        var response = MovimentacaoItemMapper.toResponseDTO(movimentacao);

        assertThat(response.instanciaIdentificacao()).isEqualTo("SER-10");
        assertThat(response.localOrigemNome()).isEqualTo("Origem");
        assertThat(response.localDestinoNome()).isEqualTo("Destino");
        assertThat(response.motivo()).isEqualTo("Organizacao");
    }

    private Categoria categoria(String nome, String icone) {
        Categoria categoria = new Categoria();
        categoria.setId(UUID.randomUUID());
        categoria.setNome(nome);
        categoria.setIcone(icone);
        categoria.setAtivo(true);
        return categoria;
    }

    private ItemMestre item(String nome, Categoria categoria) {
        ItemMestre item = new ItemMestre();
        item.setId(UUID.randomUUID());
        item.setNome(nome);
        item.setCategoria(categoria);
        item.setAtivo(true);
        return item;
    }

    private LocalArmazenamento local(String nome, LocalArmazenamento pai) {
        LocalArmazenamento local = new LocalArmazenamento();
        local.setId(UUID.randomUUID());
        local.setNome(nome);
        local.setPai(pai);
        local.setAtivo(true);
        return local;
    }
}

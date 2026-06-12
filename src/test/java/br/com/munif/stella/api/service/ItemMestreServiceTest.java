package br.com.munif.stella.api.service;

import br.com.munif.stella.api.dto.ItemMestreCreateDTO;
import br.com.munif.stella.api.dto.ImagemItemMestreDTO;
import br.com.munif.stella.api.dto.ItemMestreUpdateDTO;
import br.com.munif.stella.api.entity.Categoria;
import br.com.munif.stella.api.entity.ItemMestre;
import br.com.munif.stella.api.repository.CategoriaRepository;
import br.com.munif.stella.api.repository.ItemMestreRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemMestreServiceTest {

    @Mock
    private ItemMestreRepository repository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ImagemItemMestreStorageService imagemStorageService;

    @Mock
    private ItemMestreVectorSearchService vectorSearchService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ItemMestreService service;

    @Test
    void deveCriarItemMestreComCategoriaNormalizandoCampos() {
        UUID categoriaId = UUID.randomUUID();
        Categoria categoria = categoria(categoriaId, "Eletronicos", "eletronicos", true);

        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(repository.save(any(ItemMestre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.criar(new ItemMestreCreateDTO(
                "  Notebook Dell Latitude 5440  ",
                "  Notebook corporativo  ",
                "  Preparado para futuras instancias  ",
                null,
                categoriaId,
                true
        ));

        ArgumentCaptor<ItemMestre> captor = ArgumentCaptor.forClass(ItemMestre.class);
        verify(repository).save(captor.capture());

        ItemMestre itemSalvo = captor.getValue();
        assertThat(itemSalvo.getNome()).isEqualTo("Notebook Dell Latitude 5440");
        assertThat(itemSalvo.getDescricao()).isEqualTo("Notebook corporativo");
        assertThat(itemSalvo.getObservacoes()).isEqualTo("Preparado para futuras instancias");
        assertThat(itemSalvo.getCategoria()).isEqualTo(categoria);
        assertThat(resposta.categoriaId()).isEqualTo(categoriaId);
        assertThat(resposta.categoriaNome()).isEqualTo("Eletronicos");
        assertThat(resposta.categoriaIcone()).isEqualTo("eletronicos");
        verify(vectorSearchService).sincronizar(itemSalvo);
    }

    @Test
    void deveCriarItemMestreSemCategoria() {
        when(repository.save(any(ItemMestre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.criar(new ItemMestreCreateDTO("Cadeira ergonomica", null, null, null, null, true));

        assertThat(resposta.nome()).isEqualTo("Cadeira ergonomica");
        assertThat(resposta.categoriaId()).isNull();
        verify(categoriaRepository, never()).findById(any(UUID.class));
    }

    @Test
    void deveCriarItemMestreInativoAposPersistenciaInicial() {
        when(repository.save(any(ItemMestre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.criar(new ItemMestreCreateDTO("Arquivo legado", null, null, null, null, false));

        assertThat(resposta.ativa()).isFalse();
        verify(repository).flush();
    }

    @Test
    void deveAtualizarItemMestreComCategoria() {
        UUID id = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        ItemMestre item = item(id, "Antigo", null);
        Categoria categoria = categoria(categoriaId, "Moveis", "moveis", true);

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(repository.save(any(ItemMestre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizar(id, new ItemMestreUpdateDTO(
                "  Cadeira ergonomica  ",
                "  Cadeira de escritorio  ",
                null,
                null,
                categoriaId,
                false
        ));

        assertThat(resposta.nome()).isEqualTo("Cadeira ergonomica");
        assertThat(resposta.descricao()).isEqualTo("Cadeira de escritorio");
        assertThat(resposta.categoriaNome()).isEqualTo("Moveis");
        assertThat(resposta.ativa()).isFalse();
        verify(vectorSearchService).sincronizar(item);
    }

    @Test
    void deveAtualizarItemMestreRemovendoCategoria() {
        UUID id = UUID.randomUUID();
        ItemMestre item = item(id, "Notebook", categoria(UUID.randomUUID(), "Eletronicos", null, true));

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(repository.save(any(ItemMestre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizar(id, new ItemMestreUpdateDTO(" Notebook ", " ", " Observacao ", null, null, true));

        assertThat(resposta.nome()).isEqualTo("Notebook");
        assertThat(resposta.descricao()).isNull();
        assertThat(resposta.observacoes()).isEqualTo("Observacao");
        assertThat(resposta.categoriaId()).isNull();
        verify(categoriaRepository, never()).findById(any(UUID.class));
    }

    @Test
    void deveImpedirCategoriaInativaNoItemMestre() {
        UUID categoriaId = UUID.randomUUID();
        Categoria categoria = categoria(categoriaId, "Inativa", null, false);

        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));

        assertThatThrownBy(() -> service.criar(new ItemMestreCreateDTO("Furadeira", null, null, null, categoriaId, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoria deve estar ativa");

        verify(repository, never()).save(any(ItemMestre.class));
    }

    @Test
    void deveImpedirCategoriaInexistenteNoItemMestre() {
        UUID categoriaId = UUID.randomUUID();

        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(new ItemMestreCreateDTO("Furadeira", null, null, null, categoriaId, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Categoria não encontrada.");

        verify(repository, never()).save(any(ItemMestre.class));
    }

    @Test
    void deveListarResumoEInativos() {
        ItemMestre ativo = item(UUID.randomUUID(), "Notebook", null);
        ItemMestre inativo = item(UUID.randomUUID(), "Arquivo legado", null);
        inativo.setAtivo(false);

        when(repository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(ativo));
        when(repository.listarTodosIncluindoInativos()).thenReturn(List.of(ativo, inativo));

        assertThat(service.listarResumo()).extracting("nome").containsExactly("Notebook");
        assertThat(service.listarResumoIncluindoInativos()).extracting("ativa").containsExactly(true, false);
    }

    @Test
    void deveBuscarPorNomeSomenteQuandoFiltroInformado() {
        ItemMestre item = item(UUID.randomUUID(), "Furadeira Bosch", null);

        when(repository.findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc("Furadeira")).thenReturn(List.of(item));

        assertThat(service.buscarPorNome("  ")).isEmpty();
        assertThat(service.buscarPorNome(" Furadeira ")).hasSize(1);
    }

    @Test
    void deveFiltrarPorNomeECategoria() {
        UUID categoriaId = UUID.randomUUID();
        Categoria categoria = categoria(categoriaId, "Eletronicos", "eletronicos", true);
        ItemMestre item = item(UUID.randomUUID(), "Notebook", categoria);

        when(repository.filtrarAtivos("Notebook", categoriaId)).thenReturn(List.of(item));

        var resposta = service.filtrar(" Notebook ", categoriaId);

        assertThat(resposta).hasSize(1);
        assertThat(resposta.getFirst().nome()).isEqualTo("Notebook");
    }

    @Test
    void deveAtualizarImagemPrincipalDoItemMestre() {
        UUID id = UUID.randomUUID();
        ItemMestre item = item(id, "Notebook", null);
        item.setImagemBucket("stella-itens");
        item.setImagemObjectKey("itens-mestre/antiga.jpg");
        var arquivo = new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[]{1, 2, 3});
        var imagem = new ImagemItemMestreDTO("stella-itens", "itens-mestre/nova.png", "image/png", 3L);

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(imagemStorageService.armazenar(id, arquivo)).thenReturn(imagem);
        when(repository.save(any(ItemMestre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizarImagemPrincipal(id, arquivo);

        assertThat(resposta.imagemUrl()).isEqualTo("/api/public/itens-mestre/%s/imagem-principal".formatted(id));
        assertThat(resposta.imagemContentType()).isEqualTo("image/png");
        assertThat(resposta.imagemTamanhoBytes()).isEqualTo(3L);
        assertThat(resposta.imagemGeneratedByAi()).isFalse();
        assertThat(resposta.imagemProvider()).isNull();
        verify(imagemStorageService).removerSilenciosamente("stella-itens", "itens-mestre/antiga.jpg");
    }

    @Test
    void deveAtualizarImagemPrincipalMarcandoOrigemIa() {
        UUID id = UUID.randomUUID();
        ItemMestre item = item(id, "Notebook", null);
        var arquivo = new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[]{1, 2, 3});
        var imagem = new ImagemItemMestreDTO("stella-itens", "itens-mestre/ia.png", "image/png", 3L);

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(imagemStorageService.armazenar(id, arquivo)).thenReturn(imagem);
        when(repository.save(any(ItemMestre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizarImagemPrincipal(id, arquivo, true, " openai ");

        assertThat(resposta.imagemGeneratedByAi()).isTrue();
        assertThat(resposta.imagemProvider()).isEqualTo("openai");
    }

    @Test
    void deveRemoverIndiceVetorialAoExcluirLogicamente() {
        UUID id = UUID.randomUUID();
        ItemMestre item = item(id, "Notebook", null);

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(repository.save(item)).thenReturn(item);

        service.excluirLogicamente(id);

        assertThat(item.isAtivo()).isFalse();
        verify(vectorSearchService).remover(id);
    }

    @Test
    void deveBuscarMetadadosEAbrirImagemPrincipal() {
        UUID id = UUID.randomUUID();
        ItemMestre item = item(id, "Notebook", null);
        item.setImagemBucket("stella-itens");
        item.setImagemObjectKey("itens-mestre/%s/foto.png".formatted(id));
        item.setImagemContentType("image/png");
        item.setImagemTamanhoBytes(3L);

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(imagemStorageService.abrir("stella-itens", "itens-mestre/%s/foto.png".formatted(id)))
                .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        var metadados = service.buscarMetadadosImagemPrincipal(id);
        var imagem = service.abrirImagemPrincipal(id);

        assertThat(metadados.objectKey()).isEqualTo("itens-mestre/%s/foto.png".formatted(id));
        assertThat(metadados.tamanhoBytes()).isEqualTo(3L);
        assertThat(imagem).hasSameContentAs(new ByteArrayInputStream(new byte[]{1, 2, 3}));
    }

    @Test
    void deveRejeitarMetadadosQuandoItemMestreNaoPossuiImagemPrincipal() {
        UUID id = UUID.randomUUID();
        ItemMestre item = item(id, "Notebook", null);

        when(repository.findById(id)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.buscarMetadadosImagemPrincipal(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item mestre não possui imagem principal.");
    }

    private ItemMestre item(UUID id, String nome, Categoria categoria) {
        ItemMestre item = new ItemMestre();
        item.setId(id);
        item.setNome(nome);
        item.setCategoria(categoria);
        item.setAtivo(true);
        return item;
    }

    private Categoria categoria(UUID id, String nome, String icone, boolean ativa) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNome(nome);
        categoria.setIcone(icone);
        categoria.setAtivo(ativa);
        return categoria;
    }
}

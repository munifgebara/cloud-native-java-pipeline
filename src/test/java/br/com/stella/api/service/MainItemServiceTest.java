package br.com.stella.api.service;

import br.com.stella.api.dto.MainItemCreateDTO;
import br.com.stella.api.exception.ExternalIntegrationException;
import br.com.stella.api.dto.ImagemItemMestreDTO;
import br.com.stella.api.dto.MainItemUpdateDTO;
import br.com.stella.api.entity.Category;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.repository.CategoryRepository;
import br.com.stella.api.repository.MainItemRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemMestreServiceTest {

    @Mock
    private MainItemRepository repository;

    @Mock
    private CategoryRepository categoriaRepository;

    @Mock
    private MainItemImageStorageService imagemStorageService;

    @Mock
    private MainItemVectorSearchService vectorSearchService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private MainItemService service;

    @Test
    void deveCriarItemMestreComCategoriaNormalizandoCampos() {
        UUID categoriaId = UUID.randomUUID();
        Category category = category(categoriaId, "Eletronicos", "eletronicos", true);

        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(category));
        when(repository.save(any(MainItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.criar(new MainItemCreateDTO(
                "  Notebook Dell Latitude 5440  ",
                "  Notebook corporativo  ",
                "  Prepared for future instances  ",
                null,
                categoriaId,
                true
        ));

        ArgumentCaptor<MainItem> captor = ArgumentCaptor.forClass(MainItem.class);
        verify(repository).save(captor.capture());

        MainItem itemSalvo = captor.getValue();
        assertThat(itemSalvo.getName()).isEqualTo("Notebook Dell Latitude 5440");
        assertThat(itemSalvo.getDescription()).isEqualTo("Notebook corporativo");
        assertThat(itemSalvo.getNotes()).isEqualTo("Prepared for future instances");
        assertThat(itemSalvo.getCategory()).isEqualTo(category);
        assertThat(resposta.categoriaId()).isEqualTo(categoriaId);
        assertThat(resposta.categoriaNome()).isEqualTo("Eletronicos");
        assertThat(resposta.categoriaIcone()).isEqualTo("eletronicos");
        verify(vectorSearchService).sincronizar(itemSalvo);
    }

    @Test
    void deveCriarItemMestreSemCategoria() {
        when(repository.save(any(MainItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.criar(new MainItemCreateDTO("Cadeira ergonomica", null, null, null, null, true));

        assertThat(resposta.nome()).isEqualTo("Cadeira ergonomica");
        assertThat(resposta.categoriaId()).isNull();
        verify(categoriaRepository, never()).findById(any(UUID.class));
    }

    @Test
    void deveCriarItemMestreMesmoQuandoIndiceVetorialFalha() {
        when(repository.save(any(MainItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new ExternalIntegrationException("pgvector unavailable"))
                .when(vectorSearchService).sincronizar(any(MainItem.class));

        var resposta = service.criar(new MainItemCreateDTO("Cadeira ergonomica", null, null, null, null, true));

        assertThat(resposta.nome()).isEqualTo("Cadeira ergonomica");
        verify(repository).save(any(MainItem.class));
        verify(vectorSearchService).sincronizar(any(MainItem.class));
    }

    @Test
    void deveSincronizarIndiceVetorialSomenteAposCommit() {
        when(repository.save(any(MainItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.criar(new MainItemCreateDTO("Cadeira ergonomica", null, null, null, null, true));

            verify(vectorSearchService, never()).sincronizar(any(MainItem.class));

            var synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(1);

            synchronizations.getFirst().afterCommit();

            verify(vectorSearchService).sincronizar(any(MainItem.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deveCriarItemMestreInativoAposPersistenciaInicial() {
        when(repository.save(any(MainItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.criar(new MainItemCreateDTO("Arquivo legado", null, null, null, null, false));

        assertThat(resposta.ativa()).isFalse();
        verify(repository).flush();
    }

    @Test
    void deveAtualizarItemMestreComCategoria() {
        UUID id = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        MainItem item = item(id, "Antigo", null);
        Category category = category(categoriaId, "Moveis", "moveis", true);

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(category));
        when(repository.save(any(MainItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizar(id, new MainItemUpdateDTO(
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
        MainItem item = item(id, "Notebook", category(UUID.randomUUID(), "Eletronicos", null, true));

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(repository.save(any(MainItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizar(id, new MainItemUpdateDTO(" Notebook ", " ", " Observacao ", null, null, true));

        assertThat(resposta.nome()).isEqualTo("Notebook");
        assertThat(resposta.descricao()).isNull();
        assertThat(resposta.observacoes()).isEqualTo("Observacao");
        assertThat(resposta.categoriaId()).isNull();
        verify(categoriaRepository, never()).findById(any(UUID.class));
    }

    @Test
    void deveImpedirCategoriaInativaNoItemMestre() {
        UUID categoriaId = UUID.randomUUID();
        Category category = category(categoriaId, "Inativa", null, false);

        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.criar(new MainItemCreateDTO("Furadeira", null, null, null, categoriaId, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category must be active");

        verify(repository, never()).save(any(MainItem.class));
    }

    @Test
    void deveImpedirCategoriaInexistenteNoItemMestre() {
        UUID categoriaId = UUID.randomUUID();

        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(new MainItemCreateDTO("Furadeira", null, null, null, categoriaId, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category not found.");

        verify(repository, never()).save(any(MainItem.class));
    }

    @Test
    void deveListarResumoEInativos() {
        MainItem ativo = item(UUID.randomUUID(), "Notebook", null);
        MainItem inativo = item(UUID.randomUUID(), "Arquivo legado", null);
        inativo.setActive(false);

        when(repository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(ativo));
        when(repository.findAllIncludingInactive()).thenReturn(List.of(ativo, inativo));

        assertThat(service.listarResumo()).extracting("nome").containsExactly("Notebook");
        assertThat(service.listarResumoIncluindoInativos()).extracting("ativa").containsExactly(true, false);
    }

    @Test
    void deveBuscarPorNomeSomenteQuandoFiltroInformado() {
        MainItem item = item(UUID.randomUUID(), "Furadeira Bosch", null);

        when(repository.findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc("Furadeira")).thenReturn(List.of(item));

        assertThat(service.buscarPorNome("  ")).isEmpty();
        assertThat(service.buscarPorNome(" Furadeira ")).hasSize(1);
    }

    @Test
    void deveFiltrarPorNomeECategoria() {
        UUID categoriaId = UUID.randomUUID();
        Category category = category(categoriaId, "Eletronicos", "eletronicos", true);
        MainItem item = item(UUID.randomUUID(), "Notebook", category);

        when(repository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(item));

        var resposta = service.filtrar(" Notebook ", categoriaId);

        assertThat(resposta).hasSize(1);
        assertThat(resposta.getFirst().nome()).isEqualTo("Notebook");
    }

    @Test
    void deveAtualizarImagemPrincipalDoItemMestre() {
        UUID id = UUID.randomUUID();
        MainItem item = item(id, "Notebook", null);
        item.setImagemBucket("stella-itens");
        item.setImagemObjectKey("itens-mestre/antiga.jpg");
        var arquivo = new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[]{1, 2, 3});
        var imagem = new ImagemItemMestreDTO("stella-itens", "itens-mestre/nova.png", "image/png", 3L);

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(imagemStorageService.armazenar(id, arquivo)).thenReturn(imagem);
        when(repository.save(any(MainItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
        MainItem item = item(id, "Notebook", null);
        var arquivo = new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[]{1, 2, 3});
        var imagem = new ImagemItemMestreDTO("stella-itens", "itens-mestre/ia.png", "image/png", 3L);

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(imagemStorageService.armazenar(id, arquivo)).thenReturn(imagem);
        when(repository.save(any(MainItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizarImagemPrincipal(id, arquivo, true, " openai ");

        assertThat(resposta.imagemGeneratedByAi()).isTrue();
        assertThat(resposta.imagemProvider()).isEqualTo("openai");
    }

    @Test
    void deveRemoverIndiceVetorialAoExcluirLogicamente() {
        UUID id = UUID.randomUUID();
        MainItem item = item(id, "Notebook", null);

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(repository.save(item)).thenReturn(item);

        service.excluirLogicamente(id);

        assertThat(item.isActive()).isFalse();
        verify(vectorSearchService).remover(id);
    }

    @Test
    void deveExcluirLogicamenteMesmoQuandoRemocaoDoIndiceVetorialFalha() {
        UUID id = UUID.randomUUID();
        MainItem item = item(id, "Notebook", null);

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(repository.save(item)).thenReturn(item);
        doThrow(new ExternalIntegrationException("pgvector unavailable")).when(vectorSearchService).remover(id);

        service.excluirLogicamente(id);

        assertThat(item.isActive()).isFalse();
        verify(vectorSearchService).remover(id);
    }

    @Test
    void deveBuscarMetadadosEAbrirImagemPrincipal() {
        UUID id = UUID.randomUUID();
        MainItem item = item(id, "Notebook", null);
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
        MainItem item = item(id, "Notebook", null);

        when(repository.findById(id)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.buscarMetadadosImagemPrincipal(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Main item does not have a main image.");
    }

    private MainItem item(UUID id, String nome, Category category) {
        MainItem item = new MainItem();
        item.setId(id);
        item.setName(nome);
        item.setCategory(category);
        item.setActive(true);
        return item;
    }

    private Category category(UUID id, String nome, String icone, boolean ativa) {
        Category category = new Category();
        category.setId(id);
        category.setName(nome);
        category.setIcon(icone);
        category.setActive(ativa);
        return category;
    }
}

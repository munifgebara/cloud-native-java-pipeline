package br.com.stella.api.service;

import br.com.stella.api.config.AiProperties;
import br.com.stella.api.config.EmbeddingsProperties;
import br.com.stella.api.config.OpenAiLimitsProperties;
import br.com.stella.api.config.VectorSearchProperties;
import br.com.stella.api.entity.Category;
import br.com.stella.api.entity.ItemInstance;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.entity.StorageLocation;
import br.com.stella.api.exception.AiUsageLimitException;
import br.com.stella.api.exception.ExternalIntegrationException;
import br.com.stella.api.repository.ItemInstanceRepository;
import br.com.stella.api.repository.MainItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemMestreVectorSearchServiceTest {

    private final EmbeddingProvider embeddingProvider = mock(EmbeddingProvider.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final MainItemRepository itemMestreRepository = mock(MainItemRepository.class);
    private final ItemInstanceRepository instanciaItemRepository = mock(ItemInstanceRepository.class);
    private final VectorSearchMetricsService consultaVetorialMetricasService = mock(VectorSearchMetricsService.class);
    private final MainItemEmbeddingDocumentFactory documentFactory = new MainItemEmbeddingDocumentFactory();

    @Test
    void naoDeveIndexarQuandoBuscaVetorialEstaDesabilitada() {
        var service = service(false);

        service.sincronizar(item(UUID.randomUUID(), true));

        verify(embeddingProvider, never()).gerarEmbedding(anyString());
        verifyNoInteractions(jdbcTemplate);
        verifyNoInteractions(consultaVetorialMetricasService);
    }

    @Test
    void deveGerarEmbeddingEAtualizarIndiceDoItemAtivo() {
        UUID id = UUID.randomUUID();
        MainItem item = item(id, true);
        when(embeddingProvider.gerarEmbedding(anyString())).thenReturn(new float[]{0.1f, 0.2f, 0.3f});

        service(true).sincronizar(item);

        ArgumentCaptor<String> documento = ArgumentCaptor.forClass(String.class);
        verify(embeddingProvider).gerarEmbedding(documento.capture());
        assertThat(documento.getValue()).contains("Name: Video card", "Description: Computer component", "Category: Electronics");
        verify(jdbcTemplate).update(anyString(), eq(id), eq("location"), eq("modelo-teste"), eq(3), eq(documento.getValue()), eq("[0.10000000,0.20000000,0.30000001]"));
    }

    @Test
    void deveRemoverIndiceQuandoItemEstaInativo() {
        UUID id = UUID.randomUUID();
        MainItem item = item(id, false);

        service(true).sincronizar(item);

        verify(jdbcTemplate).update(anyString(), eq(id));
        verify(embeddingProvider, never()).gerarEmbedding(anyString());
    }

    @Test
    void deveRemoverIndiceQuandoDocumentoFicaVazio() {
        UUID id = UUID.randomUUID();
        MainItem item = new MainItem();
        item.setId(id);
        item.setActive(true);

        service(true).sincronizar(item);

        verify(jdbcTemplate).update(anyString(), eq(id));
        verify(embeddingProvider, never()).gerarEmbedding(anyString());
    }

    @Test
    void devePropagarFalhaAoRemoverIndice() {
        UUID id = UUID.randomUUID();
        RuntimeException falha = new RuntimeException("database failure");
        doThrow(falha).when(jdbcTemplate).update(anyString(), eq(id));

        assertThatThrownBy(() -> service(true).remover(id))
                .isSameAs(falha);
    }

    @Test
    void deveFalharQuandoProviderRetornaDimensoesIncompativeis() {
        MainItem item = item(UUID.randomUUID(), true);
        when(embeddingProvider.gerarEmbedding(anyString())).thenReturn(new float[]{0.1f});

        assertThatThrownBy(() -> service(true).sincronizar(item))
                .isInstanceOf(ExternalIntegrationException.class)
                .hasMessage("Embeddings provider returned a vector with incompatible dimensions.");
    }

    @Test
    void deveBuscarSemanticamenteMontandoItemInstanciasLocaisESimilaridade() throws Exception {
        UUID itemId = UUID.randomUUID();
        UUID instanciaId = UUID.randomUUID();
        UUID localId = UUID.randomUUID();
        MainItem item = item(itemId, true);
        item.setImageObjectKey("itens/foto.png");
        StorageLocation location = new StorageLocation();
        location.setId(localId);
        location.setName("Caixa A");
        ItemInstance instance = new ItemInstance();
        instance.setId(instanciaId);
        instance.setMainItem(item);
        instance.setCurrentLocation(location);
        instance.setIdentifier("GPU 1");

        when(embeddingProvider.gerarEmbedding("onde encontro placa de computador")).thenReturn(new float[]{0.1f, 0.2f, 0.3f});
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any())).thenAnswer(invocation -> {
            RowMapper<?> mapper = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject("item_mestre_id", UUID.class)).thenReturn(itemId);
            when(rs.getDouble("similaridade")).thenReturn(0.87654);
            return List.of(mapper.mapRow(rs, 0));
        });
        when(itemMestreRepository.buscarComCategoriaPorIds(List.of(itemId))).thenReturn(List.of(item));
        when(instanciaItemRepository.buscarAtivasPorItemMestreIds(List.of(itemId))).thenReturn(List.of(instance));

        var resultado = service(true).buscar(" onde encontro placa de computador ");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().nome()).isEqualTo("Video card");
        assertThat(resultado.getFirst().similaridade()).isEqualTo(0.8765);
        assertThat(resultado.getFirst().imagemUrl()).isEqualTo("/api/public/itens-mestre/%s/imagem-principal".formatted(itemId));
        assertThat(resultado.getFirst().instancias()).extracting("identificador").containsExactly("GPU 1");
        assertThat(resultado.getFirst().locaisProvaveis()).extracting("nome").containsExactly("Caixa A");
        verify(consultaVetorialMetricasService).registrarConsulta("onde encontro placa de computador", 1);
    }

    @Test
    void deveRetornarListaVaziaQuandoBuscaVetorialEstaDesabilitada() {
        var resultado = service(false).buscar("placa de video");

        assertThat(resultado).isEmpty();
        verifyNoInteractions(embeddingProvider);
        verifyNoInteractions(jdbcTemplate);
        verifyNoInteractions(consultaVetorialMetricasService);
    }

    @Test
    void deveRetornarListaVaziaQuandoConsultaNaoTemResultados() {
        when(embeddingProvider.gerarEmbedding("placa de video")).thenReturn(new float[]{0.1f, 0.2f, 0.3f});
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());

        var resultado = service(true).buscar("placa de video");

        assertThat(resultado).isEmpty();
        verify(consultaVetorialMetricasService).registrarConsulta("placa de video", 0);
    }

    @Test
    void devePropagarFalhaAoBuscarSemanticamente() {
        String consultaLonga = "placa ".repeat(60);
        RuntimeException falha = new RuntimeException("query failure");
        when(embeddingProvider.gerarEmbedding(consultaLonga.trim())).thenReturn(new float[]{0.1f, 0.2f, 0.3f});
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any())).thenThrow(falha);

        assertThatThrownBy(() -> service(true).buscar(consultaLonga))
                .isSameAs(falha);
    }

    @Test
    void deveReindexarItensAtivosQuandoHabilitado() {
        when(itemMestreRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(item(UUID.randomUUID(), true), item(UUID.randomUUID(), true)));
        when(embeddingProvider.gerarEmbedding(anyString())).thenReturn(new float[]{0.1f, 0.2f, 0.3f});

        int total = service(true).reindexarItensAtivos();

        assertThat(total).isEqualTo(2);
        verify(jdbcTemplate, org.mockito.Mockito.times(2)).update(anyString(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void deveRetornarZeroAoReindexarQuandoBuscaVetorialEstaDesabilitada() {
        assertThat(service(false).reindexarItensAtivos()).isZero();

        verifyNoInteractions(itemMestreRepository);
        verifyNoInteractions(embeddingProvider);
    }

    @Test
    void devePropagarFalhaAoReindexarItensAtivos() {
        when(itemMestreRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(item(UUID.randomUUID(), true)));
        when(embeddingProvider.gerarEmbedding(anyString())).thenReturn(new float[]{0.1f});

        assertThatThrownBy(() -> service(true).reindexarItensAtivos())
                .isInstanceOf(ExternalIntegrationException.class)
                .hasMessage("Embeddings provider returned a vector with incompatible dimensions.");
    }

    @Test
    void deveBloquearEmbeddingQuandoIaEstaDesabilitada() {
        MainItem item = item(UUID.randomUUID(), true);

        assertThatThrownBy(() -> service(true, new AiProperties(false), new OpenAiLimitsProperties(null, null, null)).sincronizar(item))
                .isInstanceOf(AiUsageLimitException.class)
                .hasMessage("AI features are disabled in this environment.");

        verify(embeddingProvider, never()).gerarEmbedding(anyString());
    }

    @Test
    void deveBloquearEmbeddingQuandoLimiteDiarioFoiAtingido() {
        MainItem item = item(UUID.randomUUID(), true);

        assertThatThrownBy(() -> service(true, new AiProperties(true), new OpenAiLimitsProperties(null, null, 0)).sincronizar(item))
                .isInstanceOf(AiUsageLimitException.class)
                .hasMessage("Daily limit for OpenAI embedding generation reached.");

        verify(embeddingProvider, never()).gerarEmbedding(anyString());
    }

    private MainItemVectorSearchService service(boolean enabled) {
        return service(enabled, new AiProperties(true), new OpenAiLimitsProperties(null, null, null));
    }

    private MainItemVectorSearchService service(boolean enabled, AiProperties aiProperties, OpenAiLimitsProperties limitsProperties) {
        return new MainItemVectorSearchService(
                new VectorSearchProperties(enabled, 0.2, 10),
                new EmbeddingsProperties("location", "http://localhost:8000", "modelo-teste", 3),
                embeddingProvider,
                documentFactory,
                jdbcTemplate,
                itemMestreRepository,
                instanciaItemRepository,
                consultaVetorialMetricasService,
                new AiUsageGuard(aiProperties, limitsProperties)
        );
    }

    private MainItem item(UUID id, boolean ativo) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Electronics");
        category.setIcon("eletronicos");

        MainItem item = new MainItem();
        item.setId(id);
        item.setName("Video card");
        item.setDescription("Computer component");
        item.setNotes("PC part");
        item.setCategory(category);
        item.setActive(ativo);
        return item;
    }
}

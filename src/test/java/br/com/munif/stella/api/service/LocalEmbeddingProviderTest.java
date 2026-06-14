package br.com.munif.stella.api.service;

import br.com.munif.stella.api.config.EmbeddingsProperties;
import br.com.munif.stella.api.exception.IntegracaoExternaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalEmbeddingProviderTest {

    @Mock
    private EmbeddingModel embeddingModel;

    private LocalEmbeddingProvider provider;

    @BeforeEach
    void setUp() {
        provider = new LocalEmbeddingProvider(
                embeddingModel,
                new EmbeddingsProperties("local", "http://stella-embeddings:8000", "modelo-local", 3)
        );
    }

    @Test
    void deveEnviarTextoParaProviderLocalEConverterEmbedding() {
        when(embeddingModel.embed("placa de video")).thenReturn(new float[]{0.1f, 0.2f, 0.3f});

        float[] embedding = provider.gerarEmbedding("placa de video");

        assertThat(embedding).containsExactly(0.1f, 0.2f, 0.3f);
        verify(embeddingModel).embed("placa de video");
    }

    @Test
    void deveRetornarEmbeddingDoProviderParaDiferentesTextos() {
        when(embeddingModel.embed("notebook")).thenReturn(new float[]{0.4f, 0.5f, 0.6f});

        assertThat(provider.gerarEmbedding("notebook")).containsExactly(0.4f, 0.5f, 0.6f);
    }

    @Test
    void deveFalharQuandoProviderLancaExcecao() {
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> provider.gerarEmbedding("notebook"))
                .isInstanceOf(IntegracaoExternaException.class)
                .hasMessage("Falha ao consultar o provider local de embeddings.");
    }
}

package br.com.munif.stella.api.service;

import br.com.munif.stella.api.config.EmbeddingsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LocalEmbeddingProviderTest {

    private MockRestServiceServer server;
    private LocalEmbeddingProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new LocalEmbeddingProvider(
                builder,
                new EmbeddingsProperties("local", "http://stella-embeddings:8000", "modelo-local", 3)
        );
    }

    @Test
    void deveEnviarTextoParaProviderLocalEConverterEmbedding() {
        server.expect(once(), requestTo("http://stella-embeddings:8000/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"model\":\"modelo-local\"")))
                .andExpect(content().string(containsString("\"input\":\"placa de video\"")))
                .andRespond(withSuccess("""
                        {
                          "embedding": [0.1, 0.2, 0.3]
                        }
                        """, MediaType.APPLICATION_JSON));

        float[] embedding = provider.gerarEmbedding("placa de video");

        assertThat(embedding).containsExactly(0.1f, 0.2f, 0.3f);
        server.verify();
    }

    @Test
    void deveConverterRespostaCompativelComOpenAi() {
        server.expect(once(), requestTo("http://stella-embeddings:8000/embeddings"))
                .andRespond(withSuccess("""
                        {
                          "data": [
                            {
                              "embedding": [0.4, 0.5, 0.6]
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThat(provider.gerarEmbedding("notebook")).containsExactly(0.4f, 0.5f, 0.6f);
    }

    @Test
    void deveFalharQuandoRespostaNaoTemVetor() {
        server.expect(once(), requestTo("http://stella-embeddings:8000/embeddings"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.gerarEmbedding("notebook"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Provider local de embeddings retornou resposta sem vetor.");
    }
}

package br.com.munif.stella.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiCadastroFotoProviderTest {

    private MockRestServiceServer server;
    private MockEnvironment environment;
    private OpenAiCadastroFotoProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        environment = new MockEnvironment()
                .withProperty("OPENAI_API_KEY", "test-key")
                .withProperty("STELLA_OPENAI_MODEL", "gpt-test")
                .withProperty("STELLA_OPENAI_IMAGE_DETAIL", "low");
        provider = new OpenAiCadastroFotoProvider(builder, environment);
    }

    @Test
    void deveEnviarImagemParaOpenAiEConverterRespostaEstruturada() {
        server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andRespond(withSuccess("""
                        {
                          "output": [
                            {
                              "content": [
                                {
                                  "type": "output_text",
                                  "text": "{\\"itens\\":[{\\"nome\\":\\"Livro\\",\\"descricao\\":\\"Livro identificado na foto\\",\\"categoriaSugerida\\":\\"Livros\\",\\"marca\\":null,\\"modelo\\":null,\\"quantidade\\":1,\\"estadoConservacao\\":\\"bom\\",\\"observacoes\\":\\"Capa visivel\\",\\"confianca\\":0.82,\\"instancias\\":[{\\"identificador\\":\\"Livro 1\\",\\"patrimonio\\":null,\\"numeroSerie\\":null,\\"estadoConservacao\\":\\"bom\\",\\"observacoes\\":null,\\"confianca\\":0.82}]}],\\"mensagem\\":\\"Sugestoes geradas.\\"}"
                                }
                              ]
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = provider.sugerirCadastro(new MockMultipartFile("arquivo", "foto.png", "image/png", "imagem".getBytes()));

        assertThat(response.mensagem()).isEqualTo("Sugestoes geradas.");
        assertThat(response.itens()).hasSize(1);
        assertThat(response.itens().getFirst().nome()).isEqualTo("Livro");
        assertThat(response.itens().getFirst().instancias()).hasSize(1);
        assertThat(response.itens().getFirst().instancias().getFirst().identificador()).isEqualTo("Livro 1");
        server.verify();
    }

    @Test
    void deveFalharQuandoApiKeyNaoEstaNoAmbiente() {
        OpenAiCadastroFotoProvider providerSemChave = new OpenAiCadastroFotoProvider(
                RestClient.builder(),
                new MockEnvironment()
        );

        assertThatThrownBy(() -> providerSemChave.sugerirCadastro(new MockMultipartFile("arquivo", "foto.png", "image/png", "imagem".getBytes())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OPENAI_API_KEY não configurada no ambiente.");
    }
}

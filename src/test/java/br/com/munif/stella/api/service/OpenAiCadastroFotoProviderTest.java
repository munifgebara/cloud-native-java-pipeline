package br.com.munif.stella.api.service;

import br.com.munif.stella.api.config.AiProperties;
import br.com.munif.stella.api.config.OpenAiLimitsProperties;
import br.com.munif.stella.api.exception.AiUsageLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
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
                .withProperty("STELLA_OPENAI_MODEL", "gpt-test");
        provider = new OpenAiCadastroFotoProvider(builder, environment, guardSemLimite());
    }

    @Test
    void deveEnviarImagemParaOpenAiEConverterRespostaEstruturada() {
        server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().string(containsString("\"detail\":\"high\"")))
                .andExpect(content().string(containsString("\"type\":\"web_search_preview\"")))
                .andRespond(withSuccess("""
                        {
                          "output": [
                            {
                              "content": [
                                {
                                  "type": "output_text",
                                  "text": "{\\"itens\\":[{\\"nome\\":\\"Clean Code\\",\\"descricao\\":\\"Livro identificado pela capa\\",\\"categoriaSugerida\\":\\"Livros\\",\\"marca\\":null,\\"modelo\\":null,\\"autor\\":\\"Robert C. Martin\\",\\"editora\\":\\"Prentice Hall\\",\\"anoPublicacao\\":\\"2008\\",\\"isbn\\":\\"9780132350884\\",\\"fontePesquisa\\":\\"OpenAI web search\\",\\"identificacaoVerificada\\":true,\\"quantidade\\":1,\\"estadoConservacao\\":\\"bom\\",\\"observacoes\\":\\"Capa visivel\\",\\"confianca\\":0.82,\\"instancias\\":[{\\"identificador\\":\\"Clean Code 1\\",\\"patrimonio\\":null,\\"numeroSerie\\":null,\\"estadoConservacao\\":\\"bom\\",\\"observacoes\\":null,\\"confianca\\":0.82}]}],\\"mensagem\\":\\"Sugestoes geradas.\\"}"
                                }
                              ]
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = provider.sugerirCadastro(new MockMultipartFile("arquivo", "foto.png", "image/png", "imagem".getBytes()));

        assertThat(response.mensagem()).isEqualTo("Sugestoes geradas.");
        assertThat(response.itens()).hasSize(1);
        assertThat(response.itens().getFirst().nome()).isEqualTo("Clean Code");
        assertThat(response.itens().getFirst().autor()).isEqualTo("Robert C. Martin");
        assertThat(response.itens().getFirst().isbn()).isEqualTo("9780132350884");
        assertThat(response.itens().getFirst().identificacaoVerificada()).isTrue();
        assertThat(response.itens().getFirst().instancias()).hasSize(1);
        assertThat(response.itens().getFirst().instancias().getFirst().identificador()).isEqualTo("Clean Code 1");
        server.verify();
    }

    @Test
    void deveFalharQuandoApiKeyNaoEstaNoAmbiente() {
        OpenAiCadastroFotoProvider providerSemChave = new OpenAiCadastroFotoProvider(
                RestClient.builder(),
                new MockEnvironment(),
                guardSemLimite()
        );

        assertThatThrownBy(() -> providerSemChave.sugerirCadastro(new MockMultipartFile("arquivo", "foto.png", "image/png", "imagem".getBytes())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OPENAI_API_KEY não configurada no ambiente.");
    }

    @Test
    void deveConverterRespostaComOutputTextDireto() {
        server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
                .andRespond(withSuccess("""
                        {
                          "output_text": "{\\"itens\\":null,\\"mensagem\\":\\"Sem sugestoes.\\"}"
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = provider.sugerirCadastro(new MockMultipartFile("arquivo", "foto.png", "image/png", "imagem".getBytes()));

        assertThat(response.itens()).isNull();
        assertThat(response.mensagem()).isEqualTo("Sem sugestoes.");
    }

    @Test
    void deveRegistrarFalhaQuandoOpenAiRetornaErroHttp() {
        server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> provider.sugerirCadastro(new MockMultipartFile("arquivo", "foto.png", "image/png", "imagem".getBytes())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Falha ao consultar OpenAI para analisar a imagem.");
    }

    @Test
    void deveRegistrarFalhaQuandoOpenAiNaoRetornaTextoEstruturado() {
        server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.sugerirCadastro(new MockMultipartFile("arquivo", "foto.png", "image/png", "imagem".getBytes())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OpenAI não retornou sugestões estruturadas.");
    }

    @Test
    void deveBloquearAnaliseQuandoIaEstaDesabilitadaSemChamarOpenAi() {
        OpenAiCadastroFotoProvider providerBloqueado = new OpenAiCadastroFotoProvider(
                RestClient.builder(),
                new MockEnvironment().withProperty("OPENAI_API_KEY", "test-key"),
                new AiUsageGuard(new AiProperties(false), new OpenAiLimitsProperties(null, null, null))
        );

        assertThatThrownBy(() -> providerBloqueado.sugerirCadastro(new MockMultipartFile("arquivo", "foto.png", "image/png", "imagem".getBytes())))
                .isInstanceOf(AiUsageLimitException.class)
                .hasMessage("Recursos de IA estão desabilitados neste ambiente.");
    }

    @Test
    void deveBloquearAnaliseQuandoLimiteDiarioFoiAtingidoSemChamarOpenAi() {
        OpenAiCadastroFotoProvider providerBloqueado = new OpenAiCadastroFotoProvider(
                RestClient.builder(),
                new MockEnvironment().withProperty("OPENAI_API_KEY", "test-key"),
                new AiUsageGuard(new AiProperties(true), new OpenAiLimitsProperties(0, null, null))
        );

        assertThatThrownBy(() -> providerBloqueado.sugerirCadastro(new MockMultipartFile("arquivo", "foto.png", "image/png", "imagem".getBytes())))
                .isInstanceOf(AiUsageLimitException.class)
                .hasMessage("Limite diário de análise de imagens da OpenAI atingido.");
    }

    private AiUsageGuard guardSemLimite() {
        return new AiUsageGuard(new AiProperties(true), new OpenAiLimitsProperties(null, null, null));
    }
}

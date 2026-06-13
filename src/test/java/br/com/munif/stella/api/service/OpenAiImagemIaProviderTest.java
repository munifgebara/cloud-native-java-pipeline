package br.com.munif.stella.api.service;

import br.com.munif.stella.api.config.AiProperties;
import br.com.munif.stella.api.config.OpenAiLimitsProperties;
import br.com.munif.stella.api.dto.ImagemIaRequestDTO;
import br.com.munif.stella.api.exception.AiUsageLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiImagemIaProviderTest {

    private MockRestServiceServer server;
    private OpenAiImagemIaProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var environment = new MockEnvironment()
                .withProperty("OPENAI_API_KEY", "test-key")
                .withProperty("STELLA_OPENAI_IMAGE_MODEL", "gpt-image-test")
                .withProperty("STELLA_OPENAI_IMAGE_OUTPUT_FORMAT", "png");
        provider = new OpenAiImagemIaProvider(builder, environment, guardSemLimite());
    }

    @Test
    void deveEnviarPromptParaOpenAiEConverterImagemBase64() {
        server.expect(once(), requestTo("https://api.openai.com/v1/images/generations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().string(containsString("\"model\":\"gpt-image-test\"")))
                .andExpect(content().string(containsString("Nome do item: Furadeira")))
                .andExpect(content().string(containsString("\"output_format\":\"png\"")))
                .andRespond(withSuccess("""
                        {
                          "data": [
                            {
                              "b64_json": "aW1hZ2Vt"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = provider.gerarImagem(new ImagemIaRequestDTO("Furadeira", "Ferramentas", "Furadeira de impacto"));

        assertThat(response.dataUrl()).isEqualTo("data:image/png;base64,aW1hZ2Vt");
        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.provider()).isEqualTo("openai");
        server.verify();
    }

    @Test
    void deveFalharQuandoApiKeyNaoEstaNoAmbiente() {
        OpenAiImagemIaProvider providerSemChave = new OpenAiImagemIaProvider(RestClient.builder(), new MockEnvironment(), guardSemLimite());

        assertThatThrownBy(() -> providerSemChave.gerarImagem(new ImagemIaRequestDTO("Furadeira", null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OPENAI_API_KEY não configurada no ambiente.");
    }

    @Test
    void deveTratarFalhaDeConexaoComOpenAi() {
        server.expect(once(), requestTo("https://api.openai.com/v1/images/generations"))
                .andRespond(request -> {
                    throw new java.io.IOException("sem rede");
                });

        assertThatThrownBy(() -> provider.gerarImagem(new ImagemIaRequestDTO("Furadeira", null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Não foi possível conectar à OpenAI para gerar imagem do item.");
    }

    @Test
    void deveRegistrarFalhaQuandoOpenAiNaoRetornaImagem() {
        server.expect(once(), requestTo("https://api.openai.com/v1/images/generations"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.gerarImagem(new ImagemIaRequestDTO("Furadeira", null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OpenAI retornou resposta vazia para a imagem.");
    }

    @Test
    void deveUsarContentTypeJpegQuandoConfigurado() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer jpegServer = MockRestServiceServer.bindTo(builder).build();
        OpenAiImagemIaProvider jpegProvider = new OpenAiImagemIaProvider(
                builder,
                new MockEnvironment()
                        .withProperty("OPENAI_API_KEY", "test-key")
                        .withProperty("STELLA_OPENAI_IMAGE_OUTPUT_FORMAT", "jpeg"),
                guardSemLimite()
        );
        jpegServer.expect(once(), requestTo("https://api.openai.com/v1/images/generations"))
                .andRespond(withSuccess("""
                        {
                          "data": [
                            {
                              "b64_json": "aW1hZ2Vt"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = jpegProvider.gerarImagem(new ImagemIaRequestDTO("Furadeira", null, null));

        assertThat(response.contentType()).isEqualTo("image/jpeg");
        assertThat(response.dataUrl()).startsWith("data:image/jpeg;base64,");
        jpegServer.verify();
    }

    @Test
    void deveBloquearGeracaoQuandoIaEstaDesabilitadaSemChamarOpenAi() {
        OpenAiImagemIaProvider providerBloqueado = new OpenAiImagemIaProvider(
                RestClient.builder(),
                new MockEnvironment().withProperty("OPENAI_API_KEY", "test-key"),
                new AiUsageGuard(new AiProperties(false), new OpenAiLimitsProperties(null, null, null))
        );

        assertThatThrownBy(() -> providerBloqueado.gerarImagem(new ImagemIaRequestDTO("Furadeira", null, null)))
                .isInstanceOf(AiUsageLimitException.class)
                .hasMessage("Recursos de IA estão desabilitados neste ambiente.");
    }

    @Test
    void deveBloquearGeracaoQuandoLimiteDiarioFoiAtingidoSemChamarOpenAi() {
        OpenAiImagemIaProvider providerBloqueado = new OpenAiImagemIaProvider(
                RestClient.builder(),
                new MockEnvironment().withProperty("OPENAI_API_KEY", "test-key"),
                new AiUsageGuard(new AiProperties(true), new OpenAiLimitsProperties(null, 0, null))
        );

        assertThatThrownBy(() -> providerBloqueado.gerarImagem(new ImagemIaRequestDTO("Furadeira", null, null)))
                .isInstanceOf(AiUsageLimitException.class)
                .hasMessage("Limite diário de geração de imagens da OpenAI atingido.");
    }

    private AiUsageGuard guardSemLimite() {
        return new AiUsageGuard(new AiProperties(true), new OpenAiLimitsProperties(null, null, null));
    }
}

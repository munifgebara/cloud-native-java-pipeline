package br.com.stella.api.service;

import br.com.stella.api.config.AiProperties;
import br.com.stella.api.config.OpenAiLimitsProperties;
import br.com.stella.api.exception.AiUsageLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAiCadastroFotoProviderTest {

    @Mock
    private ChatModel chatModel;

    private MockEnvironment environment;
    private OpenAiCadastroFotoProvider provider;

    @BeforeEach
    void setUp() {
        lenient().when(chatModel.getOptions()).thenReturn(OpenAiChatOptions.builder().model("gpt-test").build());
        environment = new MockEnvironment()
                .withProperty("OPENAI_API_KEY", "test-key")
                .withProperty("STELLA_OPENAI_MODEL", "gpt-test");
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        provider = new OpenAiCadastroFotoProvider(chatClient, environment, guardSemLimite());
    }

    @Test
    void deveEnviarImagemParaOpenAiEConverterRespostaEstruturada() {
        when(chatModel.call(any(Prompt.class))).thenReturn(respostaJson("""
                {"itens":[{"nome":"Clean Code","descricao":"Livro identificado pela capa","categoriaSugerida":"Livros",\
                "marca":null,"modelo":null,"autor":"Robert C. Martin","editora":"Prentice Hall",\
                "anoPublicacao":"2008","isbn":"9780132350884","fontePesquisa":"conhecimento do modelo",\
                "identificacaoVerificada":true,"quantidade":1,"estadoConservacao":"bom",\
                "observacoes":"Capa visivel","confianca":0.82,\
                "instancias":[{"identificador":"Clean Code 1","patrimonio":null,"numeroSerie":null,\
                "estadoConservacao":"bom","observacoes":null,"confianca":0.82}]}],\
                "mensagem":"Sugestoes geradas."}
                """));

        var response = provider.sugerirCadastro(new MockMultipartFile("arquivo", "foto.png", "image/png", "imagem".getBytes()));

        assertThat(response.mensagem()).isEqualTo("Sugestoes geradas.");
        assertThat(response.itens()).hasSize(1);
        assertThat(response.itens().getFirst().nome()).isEqualTo("Clean Code");
        assertThat(response.itens().getFirst().autor()).isEqualTo("Robert C. Martin");
        assertThat(response.itens().getFirst().isbn()).isEqualTo("9780132350884");
        assertThat(response.itens().getFirst().identificacaoVerificada()).isTrue();
        assertThat(response.itens().getFirst().instancias()).hasSize(1);
        assertThat(response.itens().getFirst().instancias().getFirst().identificador()).isEqualTo("Clean Code 1");
    }

    @Test
    void deveFalharQuandoApiKeyNaoEstaNoAmbiente() {
        OpenAiCadastroFotoProvider providerSemChave = new OpenAiCadastroFotoProvider(
                ChatClient.builder(chatModel).build(),
                new MockEnvironment(),
                guardSemLimite()
        );

        assertThatThrownBy(() -> providerSemChave.sugerirCadastro(new MockMultipartFile("arquivo", "foto.png", "image/png", "imagem".getBytes())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OPENAI_API_KEY not configured in the environment.");

        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void deveConverterRespostaComListaVazia() {
        when(chatModel.call(any(Prompt.class))).thenReturn(respostaJson(
                "{\"itens\":null,\"mensagem\":\"Without sugestoes.\"}"
        ));

        var response = provider.sugerirCadastro(new MockMultipartFile("arquivo", "foto.png", "image/png", "imagem".getBytes()));

        assertThat(response.itens()).isNull();
        assertThat(response.mensagem()).isEqualTo("Without sugestoes.");
    }

    @Test
    void deveRegistrarFalhaQuandoOpenAiLancaExcecao() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("Fail na API"));

        assertThatThrownBy(() -> provider.sugerirCadastro(new MockMultipartFile("arquivo", "foto.png", "image/png", "imagem".getBytes())))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Fail na API");
    }

    @Test
    void deveBloquearAnaliseQuandoIaEstaDesabilitadaSemChamarOpenAi() {
        OpenAiCadastroFotoProvider providerBloqueado = new OpenAiCadastroFotoProvider(
                ChatClient.builder(chatModel).build(),
                new MockEnvironment().withProperty("OPENAI_API_KEY", "test-key"),
                new AiUsageGuard(new AiProperties(false), new OpenAiLimitsProperties(null, null, null))
        );

        assertThatThrownBy(() -> providerBloqueado.sugerirCadastro(new MockMultipartFile("arquivo", "foto.png", "image/png", "imagem".getBytes())))
                .isInstanceOf(AiUsageLimitException.class)
                .hasMessage("AI features are disabled in this environment.");

        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void deveBloquearAnaliseQuandoLimiteDiarioFoiAtingidoSemChamarOpenAi() {
        OpenAiCadastroFotoProvider providerBloqueado = new OpenAiCadastroFotoProvider(
                ChatClient.builder(chatModel).build(),
                new MockEnvironment().withProperty("OPENAI_API_KEY", "test-key"),
                new AiUsageGuard(new AiProperties(true), new OpenAiLimitsProperties(0, null, null))
        );

        assertThatThrownBy(() -> providerBloqueado.sugerirCadastro(new MockMultipartFile("arquivo", "foto.png", "image/png", "imagem".getBytes())))
                .isInstanceOf(AiUsageLimitException.class)
                .hasMessage("Daily limit for OpenAI image analysis reached.");

        verify(chatModel, never()).call(any(Prompt.class));
    }

    private ChatResponse respostaJson(String json) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(json))));
    }

    private AiUsageGuard guardSemLimite() {
        return new AiUsageGuard(new AiProperties(true), new OpenAiLimitsProperties(null, null, null));
    }
}

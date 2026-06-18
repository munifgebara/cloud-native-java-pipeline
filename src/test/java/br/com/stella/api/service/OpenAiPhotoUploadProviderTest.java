package br.com.stella.api.service;

import br.com.stella.api.config.AiProperties;
import br.com.stella.api.config.OpenAiLimitsProperties;
import br.com.stella.api.exception.AiUsageLimitException;
import br.com.stella.api.exception.ExternalIntegrationException;
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
class OpenAiPhotoUploadProviderTest {

    @Mock
    private ChatModel chatModel;

    private MockEnvironment environment;
    private OpenAiPhotoUploadProvider provider;

    @BeforeEach
    void setUp() {
        lenient().when(chatModel.getOptions()).thenReturn(OpenAiChatOptions.builder().model("gpt-test").build());
        environment = new MockEnvironment()
                .withProperty("OPENAI_API_KEY", "test-key")
                .withProperty("STELLA_OPENAI_MODEL", "gpt-test");
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        provider = new OpenAiPhotoUploadProvider(chatClient, environment, guardSemLimite());
    }

    @Test
    void shouldSendImageForOpenAiAndConvertResponseStructured() {
        when(chatModel.call(any(Prompt.class))).thenReturn(respostaJson("""
                {"items":[{"name":"Clean Code","description":"Livro identificado pela capa","suggestedCategory":"Livros",\
                "brand":null,"model":null,"author":"Robert C. Martin","publisher":"Prentice Hall",\
                "publicationYear":"2008","isbn":"9780132350884","source":"conhecimento do model",\
                "identificationVerified":true,"quantity":1,"condition":"bom",\
                "notes":"Capa visivel","confidence":0.82,\
                "instances":[{"identifier":"Clean Code 1","assetTag":null,"serialNumber":null,\
                "condition":"bom","notes":null,"confidence":0.82}]}],\
                "message":"Sugestoes geradas."}
                """));

        var response = provider.suggestRegistration(new MockMultipartFile("file", "photo.png", "image/png", "image".getBytes()));

        assertThat(response.message()).isEqualTo("Sugestoes geradas.");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().name()).isEqualTo("Clean Code");
        assertThat(response.items().getFirst().author()).isEqualTo("Robert C. Martin");
        assertThat(response.items().getFirst().isbn()).isEqualTo("9780132350884");
        assertThat(response.items().getFirst().identificationVerified()).isTrue();
        assertThat(response.items().getFirst().instances()).hasSize(1);
        assertThat(response.items().getFirst().instances().getFirst().identifier()).isEqualTo("Clean Code 1");
    }

    @Test
    void shouldFailWhenApiKeyNotIsInEnvironment() {
        OpenAiPhotoUploadProvider providerSemChave = new OpenAiPhotoUploadProvider(
                ChatClient.builder(chatModel).build(),
                new MockEnvironment(),
                guardSemLimite()
        );

        assertThatThrownBy(() -> providerSemChave.suggestRegistration(new MockMultipartFile("file", "photo.png", "image/png", "image".getBytes())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OPENAI_API_KEY not configured in the environment.");

        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void shouldConvertResponseWithListEmpty() {
        when(chatModel.call(any(Prompt.class))).thenReturn(respostaJson(
                "{\"items\":null,\"message\":\"Without suggestions.\"}"
        ));

        var response = provider.suggestRegistration(new MockMultipartFile("file", "photo.png", "image/png", "image".getBytes()));

        assertThat(response.items()).isNull();
        assertThat(response.message()).isEqualTo("Without suggestions.");
    }

    @Test
    void shouldRegisterFailureWhenOpenAiThrowsException() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("Fail na API"));

        assertThatThrownBy(() -> provider.suggestRegistration(new MockMultipartFile("file", "photo.png", "image/png", "image".getBytes())))
                .isInstanceOf(ExternalIntegrationException.class) // -> 502, not a generic 500
                .hasMessageContaining("OpenAI")
                .hasRootCauseMessage("Fail na API");
    }

    @Test
    void shouldBlockAnalysisWhenIaIsDisabledWithoutCallOpenAi() {
        OpenAiPhotoUploadProvider providerBloqueado = new OpenAiPhotoUploadProvider(
                ChatClient.builder(chatModel).build(),
                new MockEnvironment().withProperty("OPENAI_API_KEY", "test-key"),
                new AiUsageGuard(new AiProperties(false), new OpenAiLimitsProperties(null, null, null))
        );

        assertThatThrownBy(() -> providerBloqueado.suggestRegistration(new MockMultipartFile("file", "photo.png", "image/png", "image".getBytes())))
                .isInstanceOf(AiUsageLimitException.class)
                .hasMessage("AI features are disabled in this environment.");

        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void shouldBlockAnalysisWhenLimitDailyWasReachedWithoutCallOpenAi() {
        OpenAiPhotoUploadProvider providerBloqueado = new OpenAiPhotoUploadProvider(
                ChatClient.builder(chatModel).build(),
                new MockEnvironment().withProperty("OPENAI_API_KEY", "test-key"),
                new AiUsageGuard(new AiProperties(true), new OpenAiLimitsProperties(0, null, null))
        );

        assertThatThrownBy(() -> providerBloqueado.suggestRegistration(new MockMultipartFile("file", "photo.png", "image/png", "image".getBytes())))
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

package br.com.stella.api.service;

import br.com.stella.api.config.AiProperties;
import br.com.stella.api.config.OpenAiLimitsProperties;
import br.com.stella.api.dto.ImageAiRequestDTO;
import br.com.stella.api.exception.AiUsageLimitException;
import br.com.stella.api.exception.ExternalIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAiImageAiProviderTest {

    @Mock
    private ImageModel imageModel;

    private OpenAiImageAiProvider provider;

    @BeforeEach
    void setUp() {
        var environment = new MockEnvironment()
                .withProperty("OPENAI_API_KEY", "test-key")
                .withProperty("STELLA_OPENAI_IMAGE_MODEL", "gpt-image-test");
        provider = new OpenAiImageAiProvider(imageModel, environment, guardSemLimite());
    }

    @Test
    void shouldSendPromptForOpenAiAndConvertImageBase64() {
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(imageResponse("aW1hZ2Vt"));

        var response = provider.generateImage(new ImageAiRequestDTO("Furadeira", "Ferramentas", "Furadeira de impacto"));

        assertThat(response.dataUrl()).isEqualTo("data:image/png;base64,aW1hZ2Vt");
        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.provider()).isEqualTo("openai");
    }

    @Test
    void shouldIncludeNameItemInPrompt() {
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(imageResponse("abc123"));

        provider.generateImage(new ImageAiRequestDTO("Notebook", null, null));

        verify(imageModel).call(any(ImagePrompt.class));
    }

    @Test
    void shouldFailWhenApiKeyNotIsInEnvironment() {
        OpenAiImageAiProvider providerSemChave = new OpenAiImageAiProvider(imageModel, new MockEnvironment(), guardSemLimite());

        assertThatThrownBy(() -> providerSemChave.generateImage(new ImageAiRequestDTO("Furadeira", null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OPENAI_API_KEY not configured in the environment.");

        verify(imageModel, never()).call(any(ImagePrompt.class));
    }

    @Test
    void shouldHandleFailureOfConnectionWithOpenAi() {
        when(imageModel.call(any(ImagePrompt.class))).thenThrow(new RuntimeException("Could not connect to OpenAI to generate item image."));

        assertThatThrownBy(() -> provider.generateImage(new ImageAiRequestDTO("Furadeira", null, null)))
                .isInstanceOf(ExternalIntegrationException.class) // -> 502, not a generic 500
                .hasMessageContaining("OpenAI");
    }

    @Test
    void shouldRegisterFailureWhenOpenAiNotReturnsImage() {
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(new ImageResponse(List.of()));

        assertThatThrownBy(() -> provider.generateImage(new ImageAiRequestDTO("Furadeira", null, null)))
                .isInstanceOf(ExternalIntegrationException.class)
                .hasMessage("OpenAI returned an empty response for the image.");
    }

    @Test
    void shouldBlockGenerationWhenIaIsDisabledWithoutCallOpenAi() {
        OpenAiImageAiProvider providerBloqueado = new OpenAiImageAiProvider(
                imageModel,
                new MockEnvironment().withProperty("OPENAI_API_KEY", "test-key"),
                new AiUsageGuard(new AiProperties(false), new OpenAiLimitsProperties(null, null, null))
        );

        assertThatThrownBy(() -> providerBloqueado.generateImage(new ImageAiRequestDTO("Furadeira", null, null)))
                .isInstanceOf(AiUsageLimitException.class)
                .hasMessage("AI features are disabled in this environment.");

        verify(imageModel, never()).call(any(ImagePrompt.class));
    }

    @Test
    void shouldBlockGenerationWhenLimitDailyWasReachedWithoutCallOpenAi() {
        OpenAiImageAiProvider providerBloqueado = new OpenAiImageAiProvider(
                imageModel,
                new MockEnvironment().withProperty("OPENAI_API_KEY", "test-key"),
                new AiUsageGuard(new AiProperties(true), new OpenAiLimitsProperties(null, 0, null))
        );

        assertThatThrownBy(() -> providerBloqueado.generateImage(new ImageAiRequestDTO("Furadeira", null, null)))
                .isInstanceOf(AiUsageLimitException.class)
                .hasMessage("Daily limit for OpenAI image generation reached.");

        verify(imageModel, never()).call(any(ImagePrompt.class));
    }

    private ImageResponse imageResponse(String base64) {
        return new ImageResponse(List.of(new ImageGeneration(new Image(null, base64))));
    }

    private AiUsageGuard guardSemLimite() {
        return new AiUsageGuard(new AiProperties(true), new OpenAiLimitsProperties(null, null, null));
    }
}

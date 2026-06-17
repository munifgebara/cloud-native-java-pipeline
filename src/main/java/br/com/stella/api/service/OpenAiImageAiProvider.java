package br.com.stella.api.service;

import br.com.stella.api.dto.ImageAiRequestDTO;
import br.com.stella.api.dto.ImageAiResponseDTO;
import br.com.stella.api.observability.StructuredBusinessLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.stella.api.exception.ExternalIntegrationException;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class OpenAiImageAiProvider implements ImageAiProvider {

    private static final String PROVIDER = "openai";
    private static final String CONTENT_TYPE = "image/png";
    private static final Logger log = LoggerFactory.getLogger(OpenAiImageAiProvider.class);

    private final ImageModel imageModel;
    private final Environment environment;
    private final AiUsageGuard aiUsageGuard;

    public OpenAiImageAiProvider(ImageModel imageModel, Environment environment, AiUsageGuard aiUsageGuard) {
        this.imageModel = imageModel;
        this.environment = environment;
        this.aiUsageGuard = aiUsageGuard;
    }

    @Override
    public ImageAiResponseDTO generateImage(ImageAiRequestDTO request) {
        aiUsageGuard.assertEnabled(AiOperation.IMAGE_GENERATION);
        String apiKey = environment.getProperty("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY not configured in the environment.");
        }

        String modelo = modelo();
        long inicio = System.nanoTime();

        try {
            aiUsageGuard.consume(AiOperation.IMAGE_GENERATION);
            var options = OpenAiImageOptions.builder()
                    .apiKey(apiKey)
                    .model(modelo)
                    .size(environment.getProperty("STELLA_OPENAI_IMAGE_SIZE", "1024x1024"))
                    .quality(environment.getProperty("STELLA_OPENAI_IMAGE_QUALITY", "low"))
                    .n(1)
                    .build();

            ImageResponse response = imageModel.call(new ImagePrompt(prompt(request), options));

            var resultado = parseResponse(response);
            StructuredBusinessLogger.info(log, "ai", "image-generation", StructuredBusinessLogger.fields(
                    "ai_provider", PROVIDER,
                    "ai_model", modelo,
                    "duration_ms", elapsedMillis(inicio),
                    "image_content_type", resultado.contentType(),
                    "success", true
            ));
            return resultado;
        } catch (RuntimeException ex) {
            logFailure(modelo, inicio, ex);
            throw ex;
        }
    }

    private ImageAiResponseDTO parseResponse(ImageResponse response) {
        if (response == null || response.getResults().isEmpty()) {
            throw new ExternalIntegrationException("OpenAI returned an empty response for the image.");
        }

        String base64 = response.getResult().getOutput().getB64Json();
        if (base64 == null || base64.isBlank()) {
            throw new ExternalIntegrationException("OpenAI did not return the image in base64.");
        }

        return new ImageAiResponseDTO("data:%s;base64,%s".formatted(CONTENT_TYPE, base64), CONTENT_TYPE, PROVIDER);
    }

    private String modelo() {
        return environment.getProperty("STELLA_OPENAI_IMAGE_MODEL", "gpt-image-1");
    }

    private String prompt(ImageAiRequestDTO request) {
        return """
                Generate a clean catalog image to represent an inventory item.
                Show only the product, centered, well lit, without text, without invented logos and without people.
                Use neutral realistic style, simple background and appropriate framing for a thumbnail.

                Item name: %s
                Category: %s
                Description: %s
                """.formatted(
                request.nome(),
                request.category() == null ? "not provided" : request.category(),
                request.descricao() == null ? "not provided" : request.descricao()
        );
    }

    private void logFailure(String modelo, long inicio, Exception ex) {
        StructuredBusinessLogger.error(log, "ai", "image-generation", StructuredBusinessLogger.fields(
                "ai_provider", PROVIDER,
                "ai_model", modelo,
                "duration_ms", elapsedMillis(inicio),
                "success", false
        ), ex);
    }

    private long elapsedMillis(long inicio) {
        return (System.nanoTime() - inicio) / 1_000_000L;
    }
}

package br.com.stella.api.service;

import br.com.stella.api.dto.ImageAiRequestDTO;
import br.com.stella.api.dto.ImageAiResponseDTO;
import br.com.stella.api.observability.StructuredBusinessLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.stella.api.exception.AiUsageLimitException;
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

        String model = model();
        long inicio = System.nanoTime();

        try {
            aiUsageGuard.consume(AiOperation.IMAGE_GENERATION);
            var options = OpenAiImageOptions.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .size(environment.getProperty("STELLA_OPENAI_IMAGE_SIZE", "1024x1024"))
                    .quality(environment.getProperty("STELLA_OPENAI_IMAGE_QUALITY", "low"))
                    .n(1)
                    .build();

            ImageResponse response = imageModel.call(new ImagePrompt(prompt(request), options));

            var result = parseResponse(response);
            StructuredBusinessLogger.info(log, "ai", "image-generation", StructuredBusinessLogger.fields(
                    "ai_provider", PROVIDER,
                    "ai_model", model,
                    "duration_ms", elapsedMillis(inicio),
                    "image_content_type", result.contentType(),
                    "success", true
            ));
            return result;
        } catch (AiUsageLimitException | ExternalIntegrationException ex) {
            logFailure(model, inicio, ex);
            throw ex;
        } catch (RuntimeException ex) {
            // An OpenAI/transport failure (e.g. model access denied) is an external integration
            // problem -> 502, not a generic 500.
            logFailure(model, inicio, ex);
            throw new ExternalIntegrationException("Failed to generate the image via OpenAI.", ex);
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

    private String model() {
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
                request.name(),
                request.category() == null ? "not provided" : request.category(),
                request.description() == null ? "not provided" : request.description()
        );
    }

    private void logFailure(String model, long inicio, Exception ex) {
        StructuredBusinessLogger.error(log, "ai", "image-generation", StructuredBusinessLogger.fields(
                "ai_provider", PROVIDER,
                "ai_model", model,
                "duration_ms", elapsedMillis(inicio),
                "success", false
        ), ex);
    }

    private long elapsedMillis(long inicio) {
        return (System.nanoTime() - inicio) / 1_000_000L;
    }
}

package br.com.stella.api.service;

import br.com.stella.api.dto.PhotoUploadSuggestionResponseDTO;
import br.com.stella.api.exception.AiUsageLimitException;
import br.com.stella.api.exception.ExternalIntegrationException;
import br.com.stella.api.observability.StructuredBusinessLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class OpenAiPhotoUploadProvider implements PhotoUploadAiProvider {

    private static final String PROVIDER = "openai";
    private static final Logger log = LoggerFactory.getLogger(OpenAiPhotoUploadProvider.class);
    private static final String ORIENTACAO = """
            Analyze the photo and identify objects that could become inventory registrations.
            Respond only with JSON conforming to the schema.
            Separate conceptual items from physical instances.
            For multiple identical objects, return one item with quantity and equivalent instances.
            For books, try to identify title, author, publisher, year and ISBN from the cover, spine or visible text.
            Use your knowledge to validate books identifiable by cover/spine when there is sufficient text.
            Of not return generic names like "Book" when it is possible to identify the title.
            For distinct books, return a separate item for each title.
            Of not invent unreadable asset or serial numbers; use null in those cases.
            Of not invent bibliographic metadata: use null when there is insufficient visual evidence or validation.
            Use short, useful names in English.
            If there is insufficient confidence, return an empty list and a clear message.
            """;

    private final ChatClient chatClient;
    private final Environment environment;
    private final AiUsageGuard aiUsageGuard;

    @Autowired
    public OpenAiPhotoUploadProvider(ChatClient.Builder chatClientBuilder, Environment environment, AiUsageGuard aiUsageGuard) {
        this(chatClientBuilder.build(), environment, aiUsageGuard);
    }

    OpenAiPhotoUploadProvider(ChatClient chatClient, Environment environment, AiUsageGuard aiUsageGuard) {
        this.chatClient = chatClient;
        this.environment = environment;
        this.aiUsageGuard = aiUsageGuard;
    }

    @Override
    public PhotoUploadSuggestionResponseDTO suggestRegistration(MultipartFile image) {
        aiUsageGuard.assertEnabled(AiOperation.IMAGE_ANALYSIS);
        String apiKey = environment.getProperty("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY not configured in the environment.");
        }

        String model = model();
        long inicio = System.nanoTime();

        try {
            byte[] bytes = image.getBytes();
            aiUsageGuard.consume(AiOperation.IMAGE_ANALYSIS);
            var result = chatClient.prompt()
                    .options(OpenAiChatOptions.builder().apiKey(apiKey).model(model))
                    .user(u -> u.text(ORIENTACAO).media(MimeType.valueOf(image.getContentType()), new ByteArrayResource(bytes)))
                    .call()
                    .entity(PhotoUploadSuggestionResponseDTO.class);

            StructuredBusinessLogger.info(log, "ai", "image-identification", StructuredBusinessLogger.fields(
                    "ai_provider", PROVIDER,
                    "ai_model", model,
                    "duration_ms", elapsedMillis(inicio),
                    "success", true,
                    "ai_detected_items", result == null || result.items() == null ? 0 : result.items().size()
            ));
            return result;
        } catch (IOException ex) {
            logFailure(model, inicio, ex);
            throw new IllegalArgumentException("Unable to read the submitted image.", ex);
        } catch (AiUsageLimitException | ExternalIntegrationException ex) {
            logFailure(model, inicio, ex);
            throw ex;
        } catch (RuntimeException ex) {
            // OpenAI/transport failure -> 502 (external integration), not a generic 500.
            logFailure(model, inicio, ex);
            throw new ExternalIntegrationException("Failed to analyze the image via OpenAI: " + rootMessage(ex), ex);
        }
    }

    /** Deepest cause message, so the actual OpenAI error is surfaced to the caller. */
    private static String rootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    }

    private String model() {
        return environment.getProperty("STELLA_OPENAI_MODEL", "gpt-4.1-mini");
    }

    private void logFailure(String model, long inicio, Exception ex) {
        StructuredBusinessLogger.error(log, "ai", "image-identification", StructuredBusinessLogger.fields(
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

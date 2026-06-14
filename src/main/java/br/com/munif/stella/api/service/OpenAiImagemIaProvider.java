package br.com.munif.stella.api.service;

import br.com.munif.stella.api.dto.ImagemIaRequestDTO;
import br.com.munif.stella.api.dto.ImagemIaResponseDTO;
import br.com.munif.stella.api.observability.StructuredBusinessLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class OpenAiImagemIaProvider implements ImagemIaProvider {

    private static final String PROVIDER = "openai";
    private static final String CONTENT_TYPE = "image/png";
    private static final Logger log = LoggerFactory.getLogger(OpenAiImagemIaProvider.class);

    private final ImageModel imageModel;
    private final Environment environment;
    private final AiUsageGuard aiUsageGuard;

    public OpenAiImagemIaProvider(ImageModel imageModel, Environment environment, AiUsageGuard aiUsageGuard) {
        this.imageModel = imageModel;
        this.environment = environment;
        this.aiUsageGuard = aiUsageGuard;
    }

    @Override
    public ImagemIaResponseDTO gerarImagem(ImagemIaRequestDTO request) {
        aiUsageGuard.assertEnabled(AiOperation.IMAGE_GENERATION);
        String apiKey = environment.getProperty("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY não configurada no ambiente.");
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

    private ImagemIaResponseDTO parseResponse(ImageResponse response) {
        if (response == null || response.getResults().isEmpty()) {
            throw new IllegalStateException("OpenAI retornou resposta vazia para a imagem.");
        }

        String base64 = response.getResult().getOutput().getB64Json();
        if (base64 == null || base64.isBlank()) {
            throw new IllegalStateException("OpenAI não retornou a imagem em base64.");
        }

        return new ImagemIaResponseDTO("data:%s;base64,%s".formatted(CONTENT_TYPE, base64), CONTENT_TYPE, PROVIDER);
    }

    private String modelo() {
        return environment.getProperty("STELLA_OPENAI_IMAGE_MODEL", "gpt-image-1");
    }

    private String prompt(ImagemIaRequestDTO request) {
        return """
                Gere uma imagem limpa de catálogo para representar um item de inventário.
                Mostre apenas o produto, centralizado, bem iluminado, sem texto, sem logotipos inventados e sem pessoas.
                Use estilo realista neutro, fundo simples e enquadramento adequado para miniatura.

                Nome do item: %s
                Categoria: %s
                Descrição: %s
                """.formatted(
                request.nome(),
                request.categoria() == null ? "não informada" : request.categoria(),
                request.descricao() == null ? "não informada" : request.descricao()
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

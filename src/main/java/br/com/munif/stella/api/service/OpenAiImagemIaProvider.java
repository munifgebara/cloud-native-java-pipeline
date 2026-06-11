package br.com.munif.stella.api.service;

import br.com.munif.stella.api.dto.ImagemIaRequestDTO;
import br.com.munif.stella.api.dto.ImagemIaResponseDTO;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiImagemIaProvider implements ImagemIaProvider {

    private static final String API_URL = "https://api.openai.com/v1/images/generations";
    private static final String PROVIDER = "openai";

    private final RestClient restClient;
    private final Environment environment;

    public OpenAiImagemIaProvider(RestClient.Builder restClientBuilder, Environment environment) {
        this.restClient = restClientBuilder.build();
        this.environment = environment;
    }

    @Override
    public ImagemIaResponseDTO gerarImagem(ImagemIaRequestDTO request) {
        String apiKey = environment.getProperty("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY não configurada no ambiente.");
        }

        try {
            Map<String, Object> response = restClient.post()
                    .uri(API_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(request))
                    .retrieve()
                    .body(Map.class);

            return parseResponse(response);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("Falha ao consultar OpenAI para gerar imagem do item.", ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Não foi possível conectar à OpenAI para gerar imagem do item.", ex);
        }
    }

    private Map<String, Object> requestBody(ImagemIaRequestDTO request) {
        String modelo = environment.getProperty("STELLA_OPENAI_IMAGE_MODEL", "gpt-image-1");
        String size = environment.getProperty("STELLA_OPENAI_IMAGE_SIZE", "1024x1024");
        String quality = environment.getProperty("STELLA_OPENAI_IMAGE_QUALITY", "low");
        String outputFormat = environment.getProperty("STELLA_OPENAI_IMAGE_OUTPUT_FORMAT", "png");

        return Map.of(
                "model", modelo,
                "prompt", prompt(request),
                "size", size,
                "quality", quality,
                "output_format", outputFormat,
                "n", 1
        );
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

    @SuppressWarnings("unchecked")
    private ImagemIaResponseDTO parseResponse(Map<String, Object> response) {
        if (response == null || !(response.get("data") instanceof List<?> imagens) || imagens.isEmpty()) {
            throw new IllegalStateException("OpenAI retornou resposta vazia para a imagem.");
        }

        Object primeira = imagens.getFirst();
        if (!(primeira instanceof Map<?, ?> imagem) || !(imagem.get("b64_json") instanceof String base64) || base64.isBlank()) {
            throw new IllegalStateException("OpenAI não retornou a imagem em base64.");
        }

        String contentType = contentType();
        return new ImagemIaResponseDTO("data:%s;base64,%s".formatted(contentType, base64), contentType, PROVIDER);
    }

    private String contentType() {
        return switch (environment.getProperty("STELLA_OPENAI_IMAGE_OUTPUT_FORMAT", "png").toLowerCase()) {
            case "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            default -> "image/png";
        };
    }
}

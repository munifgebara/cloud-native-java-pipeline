package br.com.munif.stella.api.service;

import br.com.munif.stella.api.config.EmbeddingsProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class LocalEmbeddingProvider implements EmbeddingProvider {

    private final RestClient restClient;
    private final EmbeddingsProperties properties;

    public LocalEmbeddingProvider(RestClient.Builder restClientBuilder, EmbeddingsProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public float[] gerarEmbedding(String texto) {
        try {
            Map<String, Object> response = restClient.post()
                    .uri(embeddingsUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", properties.model(),
                            "input", texto
                    ))
                    .retrieve()
                    .body(Map.class);

            return parseEmbedding(response);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("Falha ao consultar o provider local de embeddings.", ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Não foi possível conectar ao provider local de embeddings.", ex);
        }
    }

    private String embeddingsUrl() {
        String baseUrl = properties.baseUrl();
        if (baseUrl.endsWith("/embeddings")) {
            return baseUrl;
        }
        return baseUrl.replaceAll("/+$", "") + "/embeddings";
    }

    private float[] parseEmbedding(Map<String, Object> response) {
        Object embedding = response == null ? null : response.get("embedding");
        if (embedding instanceof List<?> values) {
            return toFloatArray(values);
        }

        Object embeddings = response == null ? null : response.get("embeddings");
        if (embeddings instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof List<?> values) {
            return toFloatArray(values);
        }

        Object data = response == null ? null : response.get("data");
        if (data instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Map<?, ?> item && item.get("embedding") instanceof List<?> values) {
            return toFloatArray(values);
        }

        throw new IllegalStateException("Provider local de embeddings retornou resposta sem vetor.");
    }

    private float[] toFloatArray(List<?> values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            if (!(value instanceof Number number)) {
                throw new IllegalStateException("Provider local de embeddings retornou vetor inválido.");
            }
            result[i] = number.floatValue();
        }
        return result;
    }
}

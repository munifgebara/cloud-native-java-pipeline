package br.com.munif.stella.api.service;

import br.com.munif.stella.api.dto.CadastroFotoSugestaoResponseDTO;
import br.com.munif.stella.api.observability.StructuredBusinessLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiCadastroFotoProvider implements CadastroFotoIaProvider {

    private static final String API_URL = "https://api.openai.com/v1/responses";
    private static final String PROVIDER = "openai";
    private static final Logger log = LoggerFactory.getLogger(OpenAiCadastroFotoProvider.class);
    private static final String ORIENTACAO = """
            Analise a foto e identifique objetos que possam virar cadastros de inventário.
            Responda somente com JSON aderente ao schema.
            Separe item conceitual de instâncias físicas.
            Para múltiplos objetos iguais, retorne um item com quantidade e instâncias equivalentes.
            Para livros, tente identificar título, autor, editora, ano e ISBN pela capa, lombada ou texto visível.
            Use pesquisa na web para validar livros identificáveis pela capa/lombada quando houver texto suficiente.
            Não retorne nomes genéricos como "Livro" quando for possível identificar o título.
            Para livros distintos, retorne um item separado para cada título.
            Não invente números de patrimônio ou série ilegíveis; use null nesses casos.
            Não invente metadados bibliográficos: use null quando não houver evidência visual ou validação suficiente.
            Use nomes curtos e úteis em português do Brasil.
            Se não houver confiança suficiente, retorne lista vazia e uma mensagem clara.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Environment environment;

    public OpenAiCadastroFotoProvider(RestClient.Builder restClientBuilder, Environment environment) {
        this.restClient = restClientBuilder.build();
        this.environment = environment;
    }

    @Override
    public CadastroFotoSugestaoResponseDTO sugerirCadastro(MultipartFile imagem) {
        String apiKey = environment.getProperty("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY não configurada no ambiente.");
        }
        String modelo = modelo();
        long inicio = System.nanoTime();

        try {
            Map<String, Object> response = restClient.post()
                    .uri(API_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(imagem))
                    .retrieve()
                    .body(Map.class);

            CadastroFotoSugestaoResponseDTO resultado = parseResponse(response);
            StructuredBusinessLogger.info(log, "ai", "image-identification", StructuredBusinessLogger.fields(
                    "ai_provider", PROVIDER,
                    "ai_model", modelo,
                    "duration_ms", elapsedMillis(inicio),
                    "success", true,
                    "ai_detected_items", resultado.itens() == null ? 0 : resultado.itens().size()
            ));
            return resultado;
        } catch (RestClientResponseException ex) {
            logFailure(modelo, inicio, ex);
            throw new IllegalStateException("Falha ao consultar OpenAI para analisar a imagem.", ex);
        } catch (RestClientException ex) {
            logFailure(modelo, inicio, ex);
            throw new IllegalStateException("Não foi possível conectar à OpenAI para analisar a imagem.", ex);
        } catch (IOException ex) {
            logFailure(modelo, inicio, ex);
            throw new IllegalArgumentException("Não foi possível ler a imagem enviada.", ex);
        } catch (RuntimeException ex) {
            logFailure(modelo, inicio, ex);
            throw ex;
        }
    }

    private Map<String, Object> requestBody(MultipartFile imagem) throws IOException {
        String detail = environment.getProperty("STELLA_OPENAI_IMAGE_DETAIL", "high");
        String dataUrl = "data:%s;base64,%s".formatted(
                imagem.getContentType(),
                Base64.getEncoder().encodeToString(imagem.getBytes())
        );

        return Map.of(
                "model", modelo(),
                "input", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "input_text", "text", ORIENTACAO),
                                Map.of("type", "input_image", "image_url", dataUrl, "detail", detail)
                        )
                )),
                "tools", List.of(Map.of(
                        "type", "web_search_preview",
                        "search_context_size", "low"
                )),
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "stella_cadastro_foto",
                        "strict", true,
                        "schema", schema()
                ))
        );
    }

    private String modelo() {
        return environment.getProperty("STELLA_OPENAI_MODEL", "gpt-4.1-mini");
    }

    private void logFailure(String modelo, long inicio, Exception ex) {
        StructuredBusinessLogger.error(log, "ai", "image-identification", StructuredBusinessLogger.fields(
                "ai_provider", PROVIDER,
                "ai_model", modelo,
                "duration_ms", elapsedMillis(inicio),
                "success", false
        ), ex);
    }

    private long elapsedMillis(long inicio) {
        return (System.nanoTime() - inicio) / 1_000_000L;
    }

    private Map<String, Object> schema() {
        Map<String, Object> instancia = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("identificador", "patrimonio", "numeroSerie", "estadoConservacao", "observacoes", "confianca"),
                "properties", Map.of(
                        "identificador", nullableString(),
                        "patrimonio", nullableString(),
                        "numeroSerie", nullableString(),
                        "estadoConservacao", nullableString(),
                        "observacoes", nullableString(),
                        "confianca", Map.of("type", List.of("number", "null"), "minimum", 0, "maximum", 1)
                )
        );

        Map<String, Object> item = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("nome", "descricao", "categoriaSugerida", "marca", "modelo", "autor", "editora", "anoPublicacao", "isbn", "fontePesquisa", "identificacaoVerificada", "quantidade", "estadoConservacao", "observacoes", "confianca", "instancias"),
                "properties", Map.ofEntries(
                        Map.entry("nome", Map.of("type", "string")),
                        Map.entry("descricao", nullableString()),
                        Map.entry("categoriaSugerida", nullableString()),
                        Map.entry("marca", nullableString()),
                        Map.entry("modelo", nullableString()),
                        Map.entry("autor", nullableString()),
                        Map.entry("editora", nullableString()),
                        Map.entry("anoPublicacao", nullableString()),
                        Map.entry("isbn", nullableString()),
                        Map.entry("fontePesquisa", nullableString()),
                        Map.entry("identificacaoVerificada", Map.of("type", List.of("boolean", "null"))),
                        Map.entry("quantidade", Map.of("type", "integer", "minimum", 1)),
                        Map.entry("estadoConservacao", nullableString()),
                        Map.entry("observacoes", nullableString()),
                        Map.entry("confianca", Map.of("type", List.of("number", "null"), "minimum", 0, "maximum", 1)),
                        Map.entry("instancias", Map.of("type", "array", "items", instancia))
                )
        );

        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("itens", "mensagem"),
                "properties", Map.of(
                        "itens", Map.of("type", "array", "items", item),
                        "mensagem", nullableString()
                )
        );
    }

    private Map<String, Object> nullableString() {
        return Map.of("type", List.of("string", "null"));
    }

    private CadastroFotoSugestaoResponseDTO parseResponse(Map<String, Object> response) throws JsonProcessingException {
        if (response == null) {
            throw new IllegalStateException("OpenAI retornou resposta vazia.");
        }

        String json = outputText(response);
        if (json == null || json.isBlank()) {
            throw new IllegalStateException("OpenAI não retornou sugestões estruturadas.");
        }

        return objectMapper.readValue(json, CadastroFotoSugestaoResponseDTO.class);
    }

    @SuppressWarnings("unchecked")
    private String outputText(Map<String, Object> response) {
        Object outputText = response.get("output_text");
        if (outputText instanceof String text) {
            return text;
        }

        Object output = response.get("output");
        if (!(output instanceof List<?> outputItems)) {
            return null;
        }

        for (Object outputItem : outputItems) {
            if (!(outputItem instanceof Map<?, ?> outputMap)) {
                continue;
            }
            Object content = outputMap.get("content");
            if (!(content instanceof List<?> contentItems)) {
                continue;
            }
            for (Object contentItem : contentItems) {
                if (contentItem instanceof Map<?, ?> contentMap && contentMap.get("text") instanceof String text) {
                    return text;
                }
            }
        }

        return null;
    }
}

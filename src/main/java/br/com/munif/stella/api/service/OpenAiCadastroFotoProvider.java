package br.com.munif.stella.api.service;

import br.com.munif.stella.api.dto.CadastroFotoSugestaoResponseDTO;
import br.com.munif.stella.api.observability.StructuredBusinessLogger;
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
public class OpenAiCadastroFotoProvider implements CadastroFotoIaProvider {

    private static final String PROVIDER = "openai";
    private static final Logger log = LoggerFactory.getLogger(OpenAiCadastroFotoProvider.class);
    private static final String ORIENTACAO = """
            Analise a foto e identifique objetos que possam virar cadastros de inventário.
            Responda somente com JSON aderente ao schema.
            Separe item conceitual de instâncias físicas.
            Para múltiplos objetos iguais, retorne um item com quantidade e instâncias equivalentes.
            Para livros, tente identificar título, autor, editora, ano e ISBN pela capa, lombada ou texto visível.
            Use seu conhecimento para validar livros identificáveis pela capa/lombada quando houver texto suficiente.
            Não retorne nomes genéricos como "Livro" quando for possível identificar o título.
            Para livros distintos, retorne um item separado para cada título.
            Não invente números de patrimônio ou série ilegíveis; use null nesses casos.
            Não invente metadados bibliográficos: use null quando não houver evidência visual ou validação suficiente.
            Use nomes curtos e úteis em português do Brasil.
            Se não houver confiança suficiente, retorne lista vazia e uma mensagem clara.
            """;

    private final ChatClient chatClient;
    private final Environment environment;
    private final AiUsageGuard aiUsageGuard;

    @Autowired
    public OpenAiCadastroFotoProvider(ChatClient.Builder chatClientBuilder, Environment environment, AiUsageGuard aiUsageGuard) {
        this(chatClientBuilder.build(), environment, aiUsageGuard);
    }

    OpenAiCadastroFotoProvider(ChatClient chatClient, Environment environment, AiUsageGuard aiUsageGuard) {
        this.chatClient = chatClient;
        this.environment = environment;
        this.aiUsageGuard = aiUsageGuard;
    }

    @Override
    public CadastroFotoSugestaoResponseDTO sugerirCadastro(MultipartFile imagem) {
        aiUsageGuard.assertEnabled(AiOperation.IMAGE_ANALYSIS);
        String apiKey = environment.getProperty("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY não configurada no ambiente.");
        }

        String modelo = modelo();
        long inicio = System.nanoTime();

        try {
            byte[] bytes = imagem.getBytes();
            aiUsageGuard.consume(AiOperation.IMAGE_ANALYSIS);
            var resultado = chatClient.prompt()
                    .options(OpenAiChatOptions.builder().apiKey(apiKey).model(modelo))
                    .user(u -> u.text(ORIENTACAO).media(MimeType.valueOf(imagem.getContentType()), new ByteArrayResource(bytes)))
                    .call()
                    .entity(CadastroFotoSugestaoResponseDTO.class);

            StructuredBusinessLogger.info(log, "ai", "image-identification", StructuredBusinessLogger.fields(
                    "ai_provider", PROVIDER,
                    "ai_model", modelo,
                    "duration_ms", elapsedMillis(inicio),
                    "success", true,
                    "ai_detected_items", resultado == null || resultado.itens() == null ? 0 : resultado.itens().size()
            ));
            return resultado;
        } catch (IOException ex) {
            logFailure(modelo, inicio, ex);
            throw new IllegalArgumentException("Não foi possível ler a imagem enviada.", ex);
        } catch (RuntimeException ex) {
            logFailure(modelo, inicio, ex);
            throw ex;
        }
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
}

package br.com.munif.stella.api.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tratador global de exceções para todos os controllers REST da aplicação.
 *
 * <p>Centraliza o tratamento de erros, garantindo que todas as respostas de erro
 * sigam um formato JSON consistente:</p>
 * <pre>
 * {
 *   "timestamp": "2024-01-15T10:30:00Z",
 *   "status": 400,
 *   "codigo": "Bad Request",
 *   "erro": "Mensagem legível para o usuário",
 *   "path": "/api/v0/recurso"
 * }
 * </pre>
 *
 * <p>A anotação {@link RestControllerAdvice} faz com que o Spring intercepte
 * automaticamente qualquer exceção lançada nos controllers e chame o método
 * {@link ExceptionHandler} correspondente, sem necessidade de blocos try/catch
 * nos controllers.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Trata falhas de validação Bean Validation (anotações como {@code @NotBlank}, {@code @Size} etc.).
     *
     * <p>Retorna {@code 400 Bad Request} com a lista de campos inválidos e suas mensagens.</p>
     *
     * @param ex      exceção com os detalhes dos campos inválidos
     * @param request requisição HTTP que originou o erro
     * @return resposta 400 com mapa de erros por campo
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            erros.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Erro de validação em {} {}: {}", request.getMethod(), request.getRequestURI(), erros);

        Map<String, Object> corpo = corpo(HttpStatus.BAD_REQUEST, "Dados inválidos.", request);
        corpo.put("campos", erros);
        return ResponseEntity.badRequest().body(corpo);
    }

    /**
     * Trata registros não encontrados no banco de dados.
     * Retorna {@code 404 Not Found}.
     *
     * @param ex      exceção com a mensagem de "não encontrado"
     * @param request requisição que originou o erro
     * @return resposta 404
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> tratarNaoEncontrado(EntityNotFoundException ex, HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), ex, request, false);
    }

    /**
     * Trata tentativas de cadastro duplicado (ex.: CPF já existente).
     * Retorna {@code 409 Conflict}.
     *
     * @param ex      exceção indicando conflito de dados
     * @param request requisição que originou o erro
     * @return resposta 409
     */
    @ExceptionHandler(CadastroDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> tratarDuplicidade(CadastroDuplicadoException ex, HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage(), ex, request, false);
    }

    /**
     * Trata limites de uso de IA excedidos.
     * O status HTTP é definido pela própria exceção (pode ser {@code 429 Too Many Requests}
     * ou outro código indicado pelo serviço de IA).
     *
     * @param ex      exceção com o status e a mensagem de limite excedido
     * @param request requisição que originou o erro
     * @return resposta com o status definido pela exceção
     */
    @ExceptionHandler(AiUsageLimitException.class)
    public ResponseEntity<Map<String, Object>> tratarUsoIa(AiUsageLimitException ex, HttpServletRequest request) {
        return resposta(ex.getStatus(), ex.getMessage(), ex, request, false);
    }

    /**
     * Trata violações de regras de negócio expressas como {@link IllegalArgumentException}.
     * Retorna {@code 400 Bad Request}.
     *
     * <p>Esta é a forma preferida de sinalizar erros de validação de negócio
     * nos serviços (ex.: "CPF inválido", "Local deve estar ativo").</p>
     *
     * @param ex      exceção com a mensagem da regra violada
     * @param request requisição que originou o erro
     * @return resposta 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> tratarRegraNegocio(IllegalArgumentException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, ex.getMessage(), ex, request, false);
    }

    /**
     * Trata falhas em integrações externas (ex.: Keycloak, MinIO, OpenAI).
     * Retorna {@code 502 Bad Gateway}, indicando que o problema é em um serviço externo,
     * não na requisição do cliente.
     *
     * @param ex      exceção com detalhes da falha de integração
     * @param request requisição que originou o erro
     * @return resposta 502
     */
    @ExceptionHandler(IntegracaoExternaException.class)
    public ResponseEntity<Map<String, Object>> tratarFalhaIntegracao(IntegracaoExternaException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_GATEWAY, ex.getMessage(), ex, request, true);
    }

    /**
     * Trata estados ilegais inesperados da aplicação.
     * Retorna {@code 500 Internal Server Error} e registra o erro completo no log.
     *
     * @param ex      exceção de estado ilegal
     * @param request requisição que originou o erro
     * @return resposta 500
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> tratarEstadoIlegal(IllegalStateException ex, HttpServletRequest request) {
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado ao processar a solicitação.", ex, request, true);
    }

    /**
     * Trata erros de comunicação com o provedor de identidade (Keycloak).
     * O status HTTP é definido pela própria exceção.
     *
     * @param ex      exceção com o status e a mensagem de erro de identidade
     * @param request requisição que originou o erro
     * @return resposta com o status definido pela exceção
     */
    @ExceptionHandler(IdentidadeException.class)
    public ResponseEntity<Map<String, Object>> tratarIdentidade(IdentidadeException ex, HttpServletRequest request) {
        return resposta(ex.getStatus(), ex.getMessage(), ex, request, ex.getStatus().is5xxServerError());
    }

    /**
     * Trata violações de integridade referencial no banco de dados.
     * Retorna {@code 409 Conflict} com mensagem genérica para não expor detalhes do schema.
     *
     * @param ex      exceção de integridade relacional lançada pelo JPA/Hibernate
     * @param request requisição que originou o erro
     * @return resposta 409
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> tratarIntegridade(DataIntegrityViolationException ex, HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT,
                "Não foi possível concluir a operação por conflito com dados já existentes ou vinculados.",
                ex, request, true);
    }

    /**
     * Trata requisições malformadas: JSON inválido, parâmetro ausente ou tipo de parâmetro errado.
     * Retorna {@code 400 Bad Request}.
     *
     * @param ex      exceção de leitura ou binding da requisição
     * @param request requisição que originou o erro
     * @return resposta 400
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Map<String, Object>> tratarRequisicaoInvalida(Exception ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida. Confira os dados enviados.", ex, request, false);
    }

    /**
     * Tratador de último recurso para qualquer exceção não coberta pelos outros handlers.
     * Retorna {@code 500 Internal Server Error} e registra o stack trace completo.
     *
     * @param ex      qualquer exceção não tratada
     * @param request requisição que originou o erro
     * @return resposta 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> tratarErroInesperado(Exception ex, HttpServletRequest request) {
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado ao processar a solicitação.", ex, request, true);
    }

    /**
     * Monta a resposta de erro, registra no log e retorna o {@link ResponseEntity}.
     *
     * @param status             código HTTP da resposta
     * @param mensagem           mensagem legível para o usuário
     * @param ex                 exceção original (usada no log)
     * @param request            requisição HTTP
     * @param incluirCausaNoLog  se {@code true}, registra o stack trace completo; caso contrário, apenas a mensagem
     * @return resposta HTTP com corpo JSON padronizado
     */
    private ResponseEntity<Map<String, Object>> resposta(
            HttpStatus status,
            String mensagem,
            Exception ex,
            HttpServletRequest request,
            boolean incluirCausaNoLog
    ) {
        if (status.is5xxServerError() || incluirCausaNoLog) {
            log.error("Erro em {} {}: {}", request.getMethod(), request.getRequestURI(), mensagem, ex);
        } else {
            log.warn("Erro em {} {}: {}", request.getMethod(), request.getRequestURI(), mensagem);
        }

        return ResponseEntity.status(status).body(corpo(status, mensagem, request));
    }

    /**
     * Constrói o corpo JSON padronizado das respostas de erro.
     *
     * @param status   código HTTP
     * @param mensagem mensagem de erro
     * @param request  requisição HTTP (para extrair o path)
     * @return mapa com os campos {@code timestamp}, {@code status}, {@code codigo}, {@code erro} e {@code path}
     */
    private Map<String, Object> corpo(HttpStatus status, String mensagem, HttpServletRequest request) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", Instant.now());
        corpo.put("status", status.value());
        corpo.put("codigo", status.getReasonPhrase());
        corpo.put("erro", mensagem);
        corpo.put("path", request.getRequestURI());
        return corpo;
    }
}

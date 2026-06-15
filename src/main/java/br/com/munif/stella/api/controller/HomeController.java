package br.com.munif.stella.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST auxiliar para verificação de disponibilidade da API.
 *
 * <p>O endpoint {@code GET /api/test} confirma que a API está no ar e que
 * a autenticação JWT está funcional, retornando uma mensagem simples.</p>
 */
@RestController
public class HomeController {

    /**
     * Endpoint de sanidade para verificar que a API está operacional e autenticada.
     *
     * @return mensagem de confirmação
     */
    @GetMapping("/api/test")
    public String test() {
        return "API Stella protegida com sucesso.";
    }
}
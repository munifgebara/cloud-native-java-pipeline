package br.com.munif.stella.api.controller;

import br.com.munif.stella.api.dto.CadastroFotoSugestaoResponseDTO;
import br.com.munif.stella.api.service.CadastroFotoIaService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller REST para o fluxo de cadastro por foto assistido por IA.
 *
 * <p>Expõe o recurso {@code /api/v0/ia/cadastro-foto}. Recebe uma imagem de um bem
 * e retorna sugestões de preenchimento dos campos de cadastro (nome, categoria,
 * descrição, etc.) geradas por um modelo de visão computacional.</p>
 */
@RestController
@RequestMapping("/api/v0/ia/cadastro-foto")
public class CadastroFotoIaController {

    private final CadastroFotoIaService service;

    /**
     * Constrói o controller injetando o serviço de cadastro por foto.
     *
     * @param service serviço responsável pela análise de imagem e geração de sugestões
     */
    public CadastroFotoIaController(CadastroFotoIaService service) {
        this.service = service;
    }

    /**
     * Analisa uma imagem de um bem e retorna sugestões de preenchimento do cadastro.
     *
     * @param arquivo imagem do item enviada pelo cliente
     * @return {@code 200 OK} com sugestões de nome, categoria, descrição e demais campos
     */
    @PostMapping(value = "/sugestoes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CadastroFotoSugestaoResponseDTO> sugerirCadastro(@RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(service.sugerirCadastro(arquivo));
    }
}

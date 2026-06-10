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

@RestController
@RequestMapping("/api/v0/ia/cadastro-foto")
public class CadastroFotoIaController {

    private final CadastroFotoIaService service;

    public CadastroFotoIaController(CadastroFotoIaService service) {
        this.service = service;
    }

    @PostMapping(value = "/sugestoes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CadastroFotoSugestaoResponseDTO> sugerirCadastro(@RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(service.sugerirCadastro(arquivo));
    }
}

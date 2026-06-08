package br.com.munif.stella.api.controller;

import br.com.munif.stella.api.dto.ImagemItemMestreDTO;
import br.com.munif.stella.api.service.LocalArmazenamentoService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/public/locais")
public class PublicLocalArmazenamentoImagemController {

    private final LocalArmazenamentoService localArmazenamentoService;

    public PublicLocalArmazenamentoImagemController(LocalArmazenamentoService localArmazenamentoService) {
        this.localArmazenamentoService = localArmazenamentoService;
    }

    @GetMapping("/{id}/imagem")
    public ResponseEntity<InputStreamResource> buscarImagem(@PathVariable UUID id) {
        ImagemItemMestreDTO imagem = localArmazenamentoService.buscarMetadadosImagem(id);
        InputStream stream = localArmazenamentoService.abrirImagem(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagem.contentType()))
                .contentLength(imagem.tamanhoBytes() == null ? -1 : imagem.tamanhoBytes())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(new InputStreamResource(stream));
    }
}

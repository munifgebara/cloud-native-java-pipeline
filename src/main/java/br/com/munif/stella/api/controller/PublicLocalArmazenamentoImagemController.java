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

/**
 * Controller REST público para servir a imagem de representação de locais de armazenamento.
 *
 * <p>Expõe o endpoint {@code GET /api/public/locais/{id}/imagem}
 * sem exigir autenticação JWT, permitindo exibição direta em navegadores.</p>
 *
 * <p>As respostas incluem cabeçalho {@code Cache-Control: public, max-age=3600}
 * para cache pelo navegador por 1 hora.</p>
 */
@RestController
@RequestMapping("/api/public/locais")
public class PublicLocalArmazenamentoImagemController {

    private final LocalArmazenamentoService localArmazenamentoService;

    /**
     * Constrói o controller injetando o serviço de locais de armazenamento.
     *
     * @param localArmazenamentoService serviço responsável por recuperar metadados e streams de imagem
     */
    public PublicLocalArmazenamentoImagemController(LocalArmazenamentoService localArmazenamentoService) {
        this.localArmazenamentoService = localArmazenamentoService;
    }

    /**
     * Retorna a imagem de um local como stream binário.
     *
     * @param id UUID do local de armazenamento
     * @return {@code 200 OK} com o conteúdo da imagem e cabeçalhos de tipo e cache
     * @throws IllegalArgumentException se o local não possuir imagem cadastrada
     */
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

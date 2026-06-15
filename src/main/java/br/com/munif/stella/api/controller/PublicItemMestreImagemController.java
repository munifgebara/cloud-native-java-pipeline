package br.com.munif.stella.api.controller;

import br.com.munif.stella.api.dto.ImagemItemMestreDTO;
import br.com.munif.stella.api.service.ItemMestreService;
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
 * Controller REST público para servir a imagem principal de itens mestres.
 *
 * <p>Expõe o endpoint {@code GET /api/public/itens-mestre/{id}/imagem-principal}
 * sem exigir autenticação JWT, permitindo que a interface web exiba imagens
 * diretamente nas tags {@code <img>}.</p>
 *
 * <p>As respostas incluem cabeçalho {@code Cache-Control: public, max-age=3600}
 * para permitir cache pelo navegador por 1 hora.</p>
 */
@RestController
@RequestMapping("/api/public/itens-mestre")
public class PublicItemMestreImagemController {

    private final ItemMestreService itemMestreService;

    /**
     * Constrói o controller injetando o serviço de itens mestres.
     *
     * @param itemMestreService serviço responsável por recuperar metadados e streams de imagem
     */
    public PublicItemMestreImagemController(ItemMestreService itemMestreService) {
        this.itemMestreService = itemMestreService;
    }

    /**
     * Retorna a imagem principal de um item mestre como stream binário.
     *
     * @param id UUID do item mestre
     * @return {@code 200 OK} com o conteúdo da imagem e cabeçalhos de tipo e cache
     * @throws IllegalArgumentException se o item não possuir imagem cadastrada
     */
    @GetMapping("/{id}/imagem-principal")
    public ResponseEntity<InputStreamResource> buscarImagemPrincipal(@PathVariable UUID id) {
        ImagemItemMestreDTO imagem = itemMestreService.buscarMetadadosImagemPrincipal(id);
        InputStream stream = itemMestreService.abrirImagemPrincipal(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagem.contentType()))
                .contentLength(imagem.tamanhoBytes() == null ? -1 : imagem.tamanhoBytes())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(new InputStreamResource(stream));
    }
}

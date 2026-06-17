package br.com.stella.api.controller;

import br.com.stella.api.dto.MainItemImageDTO;
import br.com.stella.api.service.MainItemService;
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
 * Public REST controller for serving the main image of main items.
 *
 * <p>Exposes the endpoint {@code GET /api/public/itens-mestre/{id}/image-principal}
 * without requiring JWT authentication, allowing the web interface to display images
 * directly in {@code <img>} tags.</p>
 *
 * <p>Responses include the {@code Cache-Control: public, max-age=3600} header
 * to allow browser caching for 1 hour.</p>
 */
@RestController
@RequestMapping("/api/public/itens-mestre")
public class PublicMainItemImageController {

    private final MainItemService mainItemService;

    /**
     * Constructs the controller injecting the main item service.
     *
     * @param mainItemService service responsible for retrieving image metadata and streams
     */
    public PublicMainItemImageController(MainItemService mainItemService) {
        this.mainItemService = mainItemService;
    }

    /**
     * Returns the main image of a main item as a binary stream.
     *
     * @param id UUID of the main item
     * @return {@code 200 OK} with the image content and content-type and cache headers
     * @throws IllegalArgumentException if the item does not have a registered image
     */
    @GetMapping("/{id}/image-principal")
    public ResponseEntity<InputStreamResource> buscarImagemPrincipal(@PathVariable UUID id) {
        MainItemImageDTO image = mainItemService.buscarMetadadosImagemPrincipal(id);
        InputStream stream = mainItemService.abrirImagemPrincipal(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .contentLength(image.tamanhoBytes() == null ? -1 : image.tamanhoBytes())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(new InputStreamResource(stream));
    }
}

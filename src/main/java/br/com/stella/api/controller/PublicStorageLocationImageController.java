package br.com.stella.api.controller;

import br.com.stella.api.dto.MainItemImageDTO;
import br.com.stella.api.service.StorageLocationService;
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
 * Public REST controller for serving the representation image of storage locations.
 *
 * <p>Exposes the {@code GET /api/public/locais/{id}/image} endpoint
 * without requiring JWT authentication, allowing direct display in browsers.</p>
 *
 * <p>Responses include the {@code Cache-Control: public, max-age=3600} header
 * for browser caching for 1 hour.</p>
 */
@RestController
@RequestMapping("/api/public/locais")
public class PublicStorageLocationImageController {

    private final StorageLocationService storageLocationService;

    /**
     * Constructs the controller injecting the storage location service.
     *
     * @param storageLocationService service responsible for retrieving image metadata and streams
     */
    public PublicStorageLocationImageController(StorageLocationService storageLocationService) {
        this.storageLocationService = storageLocationService;
    }

    /**
     * Returns the image of a location as a binary stream.
     *
     * @param id UUID of the storage location
     * @return {@code 200 OK} with the image content and content-type and cache headers
     * @throws IllegalArgumentException if the location does not have a registered image
     */
    @GetMapping("/{id}/image")
    public ResponseEntity<InputStreamResource> buscarImagem(@PathVariable UUID id) {
        MainItemImageDTO image = storageLocationService.fetchImageMetadata(id);
        InputStream stream = storageLocationService.abrirImagem(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .contentLength(image.tamanhoBytes() == null ? -1 : image.tamanhoBytes())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(new InputStreamResource(stream));
    }
}

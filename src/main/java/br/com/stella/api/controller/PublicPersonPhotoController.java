package br.com.stella.api.controller;

import br.com.stella.api.dto.MainItemImageDTO;
import br.com.stella.api.service.PersonService;
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
@RequestMapping("/api/public/people")
public class PublicPersonPhotoController {

    private final PersonService personService;

    public PublicPersonPhotoController(PersonService personService) {
        this.personService = personService;
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<InputStreamResource> fetchPhoto(@PathVariable UUID id) {
        MainItemImageDTO image = personService.fetchPhotoMetadata(id);
        InputStream stream = personService.openPhoto(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .contentLength(image.sizeBytes() == null ? -1 : image.sizeBytes())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(new InputStreamResource(stream));
    }
}

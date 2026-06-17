package br.com.stella.api.service;

import br.com.stella.api.dto.PhotoUploadSuggestionResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhotoUploadAiServiceTest {

    private final PhotoUploadAiService service = new PhotoUploadAiService(image -> new PhotoUploadSuggestionResponseDTO(List.of(), "ok"));

    @Test
    void shouldRejectFileEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.suggestRegistration(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Please send an image for analysis.");
    }

    @Test
    void shouldRejectFormatoNotSupported() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.txt", "text/plain", "texto".getBytes());

        assertThatThrownBy(() -> service.suggestRegistration(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported image format. Use PNG, JPEG, WEBP or GIF.");
    }
}

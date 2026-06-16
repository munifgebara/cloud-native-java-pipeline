package br.com.stella.api.service;

import br.com.stella.api.dto.CadastroFotoSugestaoResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhotoUploadAiServiceTest {

    private final PhotoUploadAiService service = new PhotoUploadAiService(imagem -> new CadastroFotoSugestaoResponseDTO(List.of(), "ok"));

    @Test
    void deveRejeitarArquivoVazio() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.sugerirCadastro(arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Please send an image for analysis.");
    }

    @Test
    void deveRejeitarFormatoNaoSuportado() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "foto.txt", "text/plain", "texto".getBytes());

        assertThatThrownBy(() -> service.sugerirCadastro(arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported image format. Use PNG, JPEG, WEBP or GIF.");
    }
}

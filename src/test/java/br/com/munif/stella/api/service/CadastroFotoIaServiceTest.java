package br.com.munif.stella.api.service;

import br.com.munif.stella.api.dto.CadastroFotoSugestaoResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CadastroFotoIaServiceTest {

    private final CadastroFotoIaService service = new CadastroFotoIaService(imagem -> new CadastroFotoSugestaoResponseDTO(List.of(), "ok"));

    @Test
    void deveRejeitarArquivoVazio() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.sugerirCadastro(arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Envie uma imagem para análise.");
    }

    @Test
    void deveRejeitarFormatoNaoSuportado() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "foto.txt", "text/plain", "texto".getBytes());

        assertThatThrownBy(() -> service.sugerirCadastro(arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Formato de imagem não suportado. Use PNG, JPEG, WEBP ou GIF.");
    }
}

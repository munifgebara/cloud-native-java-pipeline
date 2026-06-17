package br.com.stella.api.service;

import br.com.stella.api.dto.ImageAiRequestDTO;
import br.com.stella.api.dto.ImageAiResponseDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageAiServiceTest {

    @Test
    void deveNormalizarRequestEDelegarParaProvider() {
        ImageAiService service = new ImageAiService(request -> {
            assertThat(request.nome()).isEqualTo("Furadeira");
            assertThat(request.category()).isEqualTo("Ferramentas");
            assertThat(request.descricao()).isNull();
            return new ImageAiResponseDTO("data:image/png;base64,abc", "image/png", "openai");
        });

        var response = service.generateImage(new ImageAiRequestDTO(" Furadeira ", " Ferramentas ", " "));

        assertThat(response.provider()).isEqualTo("openai");
    }

    @Test
    void deveExigirNomeDoItem() {
        ImageAiService service = new ImageAiService(request -> null);

        assertThatThrownBy(() -> service.generateImage(new ImageAiRequestDTO(" ", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Provide the item name to generate the image.");
    }
}

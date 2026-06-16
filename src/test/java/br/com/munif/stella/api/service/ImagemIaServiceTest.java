package br.com.munif.stella.api.service;

import br.com.munif.stella.api.dto.ImagemIaRequestDTO;
import br.com.munif.stella.api.dto.ImagemIaResponseDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagemIaServiceTest {

    @Test
    void deveNormalizarRequestEDelegarParaProvider() {
        ImagemIaService service = new ImagemIaService(request -> {
            assertThat(request.nome()).isEqualTo("Furadeira");
            assertThat(request.categoria()).isEqualTo("Ferramentas");
            assertThat(request.descricao()).isNull();
            return new ImagemIaResponseDTO("data:image/png;base64,abc", "image/png", "openai");
        });

        var response = service.gerarImagem(new ImagemIaRequestDTO(" Furadeira ", " Ferramentas ", " "));

        assertThat(response.provider()).isEqualTo("openai");
    }

    @Test
    void deveExigirNomeDoItem() {
        ImagemIaService service = new ImagemIaService(request -> null);

        assertThatThrownBy(() -> service.gerarImagem(new ImagemIaRequestDTO(" ", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Provide the item name to generate the image.");
    }
}

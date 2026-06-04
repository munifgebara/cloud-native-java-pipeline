package br.com.munif.stella.api.service;

import br.com.munif.stella.api.config.MinioProperties;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ImagemItemMestreStorageServiceTest {

    private final ImagemItemMestreStorageService service = new ImagemItemMestreStorageService(
            mock(MinioClient.class),
            new MinioProperties("http://127.0.0.1:9000", "minioadmin", "minioadmin", "stella-test", 10)
    );

    @Test
    void deveRejeitarArquivoVazio() {
        var arquivo = new MockMultipartFile("arquivo", "vazio.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.armazenar(UUID.randomUUID(), arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Informe uma imagem");
    }

    @Test
    void deveRejeitarFormatoNaoPermitido() {
        var arquivo = new MockMultipartFile("arquivo", "arquivo.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> service.armazenar(UUID.randomUUID(), arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Formato de imagem");
    }

    @Test
    void deveRejeitarImagemMaiorQueLimiteConfigurado() {
        var arquivo = new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[11]);

        assertThatThrownBy(() -> service.armazenar(UUID.randomUUID(), arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no máximo 10 bytes");
    }
}

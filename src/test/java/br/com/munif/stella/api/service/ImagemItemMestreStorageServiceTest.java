package br.com.munif.stella.api.service;

import br.com.munif.stella.api.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImagemItemMestreStorageServiceTest {

    private MinioClient minioClient;
    private ImagemItemMestreStorageService service;

    @BeforeEach
    void setUp() {
        minioClient = mock(MinioClient.class);
        service = new ImagemItemMestreStorageService(
                minioClient,
                new MinioProperties("http://127.0.0.1:9000", "minioadmin", "minioadmin", "stella-test", 10)
        );
    }

    @Test
    void deveArmazenarImagemCriandoBucketQuandoNecessario() throws Exception {
        var itemId = UUID.randomUUID();
        var arquivo = new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[]{1, 2, 3});

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        var imagem = service.armazenar(itemId, arquivo);

        assertThat(imagem.bucket()).isEqualTo("stella-test");
        assertThat(imagem.contentType()).isEqualTo("image/png");
        assertThat(imagem.tamanhoBytes()).isEqualTo(3);
        assertThat(imagem.objectKey())
                .startsWith("itens-mestre/%s/".formatted(itemId))
                .endsWith(".png");

        ArgumentCaptor<BucketExistsArgs> bucketExistsCaptor = ArgumentCaptor.forClass(BucketExistsArgs.class);
        ArgumentCaptor<MakeBucketArgs> makeBucketCaptor = ArgumentCaptor.forClass(MakeBucketArgs.class);
        ArgumentCaptor<PutObjectArgs> putObjectCaptor = ArgumentCaptor.forClass(PutObjectArgs.class);

        verify(minioClient).bucketExists(bucketExistsCaptor.capture());
        verify(minioClient).makeBucket(makeBucketCaptor.capture());
        verify(minioClient).putObject(putObjectCaptor.capture());

        assertThat(bucketExistsCaptor.getValue().bucket()).isEqualTo("stella-test");
        assertThat(makeBucketCaptor.getValue().bucket()).isEqualTo("stella-test");
        assertThat(putObjectCaptor.getValue().bucket()).isEqualTo("stella-test");
        assertThat(putObjectCaptor.getValue().object()).isEqualTo(imagem.objectKey());
        assertThat(putObjectCaptor.getValue().contentType()).isEqualTo("image/png");
    }

    @Test
    void deveArmazenarImagemDeLocalComPrefixoProprio() throws Exception {
        var localId = UUID.randomUUID();
        var arquivo = new MockMultipartFile("arquivo", "foto.webp", "image/webp", new byte[]{1, 2, 3});

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        var imagem = service.armazenarLocal(localId, arquivo);

        assertThat(imagem.bucket()).isEqualTo("stella-test");
        assertThat(imagem.contentType()).isEqualTo("image/webp");
        assertThat(imagem.tamanhoBytes()).isEqualTo(3);
        assertThat(imagem.objectKey())
                .startsWith("locais/%s/".formatted(localId))
                .endsWith(".webp");

        ArgumentCaptor<PutObjectArgs> putObjectCaptor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(putObjectCaptor.capture());

        assertThat(putObjectCaptor.getValue().bucket()).isEqualTo("stella-test");
        assertThat(putObjectCaptor.getValue().object()).isEqualTo(imagem.objectKey());
        assertThat(putObjectCaptor.getValue().contentType()).isEqualTo("image/webp");
    }

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

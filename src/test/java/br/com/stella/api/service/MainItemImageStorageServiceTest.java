package br.com.stella.api.service;

import br.com.stella.api.config.MinioProperties;
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
    private MainItemImageStorageService service;

    @BeforeEach
    void setUp() {
        minioClient = mock(MinioClient.class);
        service = new MainItemImageStorageService(
                minioClient,
                new MinioProperties("http://127.0.0.1:9000", "minioadmin", "minioadmin", "stella-test", 10)
        );
    }

    @Test
    void shouldStoreImageCreatingBucketWhenNecessary() throws Exception {
        var itemId = UUID.randomUUID();
        var file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        var image = service.armazenar(itemId, file);

        assertThat(image.bucket()).isEqualTo("stella-test");
        assertThat(image.contentType()).isEqualTo("image/png");
        assertThat(image.tamanhoBytes()).isEqualTo(3);
        assertThat(image.objectKey())
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
        assertThat(putObjectCaptor.getValue().object()).isEqualTo(image.objectKey());
        assertThat(putObjectCaptor.getValue().contentType()).isEqualTo("image/png");
    }

    @Test
    void shouldStoreImageOfLocationWithPrefixOwn() throws Exception {
        var locationId = UUID.randomUUID();
        var file = new MockMultipartFile("file", "photo.webp", "image/webp", new byte[]{1, 2, 3});

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        var image = service.store(locationId, file);

        assertThat(image.bucket()).isEqualTo("stella-test");
        assertThat(image.contentType()).isEqualTo("image/webp");
        assertThat(image.tamanhoBytes()).isEqualTo(3);
        assertThat(image.objectKey())
                .startsWith("locais/%s/".formatted(locationId))
                .endsWith(".webp");

        ArgumentCaptor<PutObjectArgs> putObjectCaptor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(putObjectCaptor.capture());

        assertThat(putObjectCaptor.getValue().bucket()).isEqualTo("stella-test");
        assertThat(putObjectCaptor.getValue().object()).isEqualTo(image.objectKey());
        assertThat(putObjectCaptor.getValue().contentType()).isEqualTo("image/webp");
    }

    @Test
    void shouldRejectFileEmpty() {
        var file = new MockMultipartFile("file", "vazio.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.armazenar(UUID.randomUUID(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provide an image");
    }

    @Test
    void shouldRejectFormatoNotAllowed() {
        var file = new MockMultipartFile("file", "file.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> service.armazenar(UUID.randomUUID(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Image format not allowed");
    }

    @Test
    void shouldRejectImageGreaterThatLimitConfigured() {
        var file = new MockMultipartFile("file", "photo.png", "image/png", new byte[11]);

        assertThatThrownBy(() -> service.armazenar(UUID.randomUUID(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 bytes");
    }
}

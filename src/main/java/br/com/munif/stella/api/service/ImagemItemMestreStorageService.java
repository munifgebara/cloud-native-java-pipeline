package br.com.munif.stella.api.service;

import br.com.munif.stella.api.config.MinioProperties;
import br.com.munif.stella.api.dto.ImagemItemMestreDTO;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ImagemItemMestreStorageService {

    private static final Set<String> CONTENT_TYPES_PERMITIDOS = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private static final Map<String, String> EXTENSOES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public ImagemItemMestreStorageService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public ImagemItemMestreDTO armazenar(UUID itemId, MultipartFile arquivo) {
        validarArquivo(arquivo);

        String contentType = arquivo.getContentType().toLowerCase(Locale.ROOT);
        String objectKey = "itens-mestre/%s/%s.%s".formatted(itemId, UUID.randomUUID(), EXTENSOES.get(contentType));

        try {
            garantirBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .stream(arquivo.getInputStream(), arquivo.getSize(), -1)
                    .contentType(contentType)
                    .build());
            return new ImagemItemMestreDTO(properties.bucket(), objectKey, contentType, arquivo.getSize());
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível armazenar a imagem no MinIO.", ex);
        }
    }

    public InputStream abrir(String bucket, String objectKey) {
        if (bucket == null || objectKey == null) {
            throw new IllegalArgumentException("Imagem não encontrada.");
        }

        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível carregar a imagem do MinIO.", ex);
        }
    }

    public void removerSilenciosamente(String bucket, String objectKey) {
        if (bucket == null || objectKey == null) {
            return;
        }

        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception ignored) {
        }
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Informe uma imagem para upload.");
        }

        if (arquivo.getSize() > properties.maxImageSizeBytes()) {
            throw new IllegalArgumentException("Imagem deve ter no máximo %d bytes.".formatted(properties.maxImageSizeBytes()));
        }

        String contentType = arquivo.getContentType();
        if (contentType == null || !CONTENT_TYPES_PERMITIDOS.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Formato de imagem não permitido. Use JPG, PNG, WebP ou GIF.");
        }
    }

    private void garantirBucket() throws Exception {
        boolean existe = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.bucket())
                .build());
        if (!existe) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(properties.bucket())
                    .build());
        }
    }
}

package br.com.stella.api.service;

import br.com.stella.api.config.MinioProperties;
import br.com.stella.api.exception.ExternalIntegrationException;
import br.com.stella.api.dto.MainItemImageDTO;
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
public class MainItemImageStorageService {

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

    public MainItemImageStorageService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public MainItemImageDTO armazenar(UUID itemId, MultipartFile file) {
        return armazenar("itens-mestre", itemId, file);
    }

    public MainItemImageDTO store(UUID locationId, MultipartFile file) {
        return armazenar("locais", locationId, file);
    }

    private MainItemImageDTO armazenar(String prefixo, UUID entidadeId, MultipartFile file) {
        validarArquivo(file);

        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String objectKey = "%s/%s/%s.%s".formatted(prefixo, entidadeId, UUID.randomUUID(), EXTENSOES.get(contentType));

        try {
            garantirBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType)
                    .build());
            return new MainItemImageDTO(properties.bucket(), objectKey, contentType, file.getSize());
        } catch (Exception ex) {
            throw new ExternalIntegrationException("Unable to store the image in MinIO.", ex);
        }
    }

    public InputStream abrir(String bucket, String objectKey) {
        if (bucket == null || objectKey == null) {
            throw new IllegalArgumentException("Image not found.");
        }

        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            throw new ExternalIntegrationException("Unable to load the image from MinIO.", ex);
        }
    }

    public void removeSilently(String bucket, String objectKey) {
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

    private void validarArquivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please provide an image for upload.");
        }

        if (file.getSize() > properties.maxImageSizeBytes()) {
            throw new IllegalArgumentException("Image must not exceed %d bytes.".formatted(properties.maxImageSizeBytes()));
        }

        String contentType = file.getContentType();
        if (contentType == null || !CONTENT_TYPES_PERMITIDOS.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Image format not allowed. Use JPG, PNG, WebP or GIF.");
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

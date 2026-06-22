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

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private static final Set<String> ALLOWED_PERSON_PHOTO_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final Map<String, String> EXTENSIONS = Map.of(
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

    public MainItemImageDTO storeMainItemImage(UUID itemId, MultipartFile file) {
        return store("main-items", itemId, file);
    }

    public MainItemImageDTO storeLocationImage(UUID locationId, MultipartFile file) {
        return store("locations", locationId, file);
    }

    public MainItemImageDTO storePersonPhoto(UUID personId, MultipartFile file) {
        return store("people", personId, file, ALLOWED_PERSON_PHOTO_CONTENT_TYPES, "Image format not allowed. Use JPG, PNG or WebP.");
    }

    private MainItemImageDTO store(String prefix, UUID entityId, MultipartFile file) {
        return store(prefix, entityId, file, ALLOWED_CONTENT_TYPES, "Image format not allowed. Use JPG, PNG, WebP or GIF.");
    }

    private MainItemImageDTO store(String prefix, UUID entityId, MultipartFile file, Set<String> allowedContentTypes, String invalidFormatMessage) {
        validateFile(file, allowedContentTypes, invalidFormatMessage);

        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String objectKey = "%s/%s/%s.%s".formatted(prefix, entityId, UUID.randomUUID(), EXTENSIONS.get(contentType));

        try {
            ensureBucket();
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

    public InputStream open(String bucket, String objectKey) {
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

    private void validateFile(MultipartFile file, Set<String> allowedContentTypes, String invalidFormatMessage) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please provide an image for upload.");
        }

        if (file.getSize() > properties.maxImageSizeBytes()) {
            throw new IllegalArgumentException("Image must not exceed %d bytes.".formatted(properties.maxImageSizeBytes()));
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(invalidFormatMessage);
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.bucket())
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(properties.bucket())
                    .build());
        }
    }
}

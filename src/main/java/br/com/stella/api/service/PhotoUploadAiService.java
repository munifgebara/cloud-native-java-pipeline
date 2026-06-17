package br.com.stella.api.service;

import br.com.stella.api.dto.PhotoUploadSuggestionResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
public class PhotoUploadAiService {

    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> CONTENT_TYPES_SUPORTADOS = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp",
            "image/gif"
    );

    private final PhotoUploadAiProvider provider;

    public PhotoUploadAiService(PhotoUploadAiProvider provider) {
        this.provider = provider;
    }

    public PhotoUploadSuggestionResponseDTO suggestRegistration(MultipartFile image) {
        validateImage(image);
        return provider.suggestRegistration(image);
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Please send an image for analysis.");
        }

        if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image must not exceed 10MB.");
        }

        if (!CONTENT_TYPES_SUPORTADOS.contains(image.getContentType())) {
            throw new IllegalArgumentException("Unsupported image format. Use PNG, JPEG, WEBP or GIF.");
        }
    }
}

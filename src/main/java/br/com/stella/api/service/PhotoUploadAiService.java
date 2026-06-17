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

    public PhotoUploadSuggestionResponseDTO sugerirCadastro(MultipartFile imagem) {
        validarImagem(imagem);
        return provider.sugerirCadastro(imagem);
    }

    private void validarImagem(MultipartFile imagem) {
        if (imagem == null || imagem.isEmpty()) {
            throw new IllegalArgumentException("Please send an image for analysis.");
        }

        if (imagem.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image must not exceed 10MB.");
        }

        if (!CONTENT_TYPES_SUPORTADOS.contains(imagem.getContentType())) {
            throw new IllegalArgumentException("Unsupported image format. Use PNG, JPEG, WEBP or GIF.");
        }
    }
}

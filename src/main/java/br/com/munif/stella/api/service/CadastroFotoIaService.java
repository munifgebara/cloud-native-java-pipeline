package br.com.munif.stella.api.service;

import br.com.munif.stella.api.dto.CadastroFotoSugestaoResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
public class CadastroFotoIaService {

    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> CONTENT_TYPES_SUPORTADOS = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp",
            "image/gif"
    );

    private final CadastroFotoIaProvider provider;

    public CadastroFotoIaService(CadastroFotoIaProvider provider) {
        this.provider = provider;
    }

    public CadastroFotoSugestaoResponseDTO sugerirCadastro(MultipartFile imagem) {
        validarImagem(imagem);
        return provider.sugerirCadastro(imagem);
    }

    private void validarImagem(MultipartFile imagem) {
        if (imagem == null || imagem.isEmpty()) {
            throw new IllegalArgumentException("Envie uma imagem para análise.");
        }

        if (imagem.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Imagem deve ter no máximo 10MB.");
        }

        if (!CONTENT_TYPES_SUPORTADOS.contains(imagem.getContentType())) {
            throw new IllegalArgumentException("Formato de imagem não suportado. Use PNG, JPEG, WEBP ou GIF.");
        }
    }
}

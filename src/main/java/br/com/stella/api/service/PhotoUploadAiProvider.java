package br.com.stella.api.service;

import br.com.stella.api.dto.PhotoUploadSuggestionResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface PhotoUploadAiProvider {

    PhotoUploadSuggestionResponseDTO sugerirCadastro(MultipartFile imagem);
}

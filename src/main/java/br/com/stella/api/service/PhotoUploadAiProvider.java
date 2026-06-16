package br.com.stella.api.service;

import br.com.stella.api.dto.CadastroFotoSugestaoResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface PhotoUploadAiProvider {

    CadastroFotoSugestaoResponseDTO sugerirCadastro(MultipartFile imagem);
}

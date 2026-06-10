package br.com.munif.stella.api.service;

import br.com.munif.stella.api.dto.CadastroFotoSugestaoResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface CadastroFotoIaProvider {

    CadastroFotoSugestaoResponseDTO sugerirCadastro(MultipartFile imagem);
}

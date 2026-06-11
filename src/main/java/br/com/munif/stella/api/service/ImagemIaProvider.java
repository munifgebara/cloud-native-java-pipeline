package br.com.munif.stella.api.service;

import br.com.munif.stella.api.dto.ImagemIaRequestDTO;
import br.com.munif.stella.api.dto.ImagemIaResponseDTO;

public interface ImagemIaProvider {

    ImagemIaResponseDTO gerarImagem(ImagemIaRequestDTO request);
}

package br.com.stella.api.service;

import br.com.stella.api.dto.ImageAiRequestDTO;
import br.com.stella.api.dto.ImageAiResponseDTO;

public interface ImageAiProvider {

    ImageAiResponseDTO gerarImagem(ImageAiRequestDTO request);
}

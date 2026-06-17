package br.com.stella.api.service;

import br.com.munif.common.utils.validacoes.BrValidations;
import br.com.stella.api.dto.ImageAiRequestDTO;
import br.com.stella.api.dto.ImageAiResponseDTO;
import org.springframework.stereotype.Service;

@Service
public class ImageAiService {

    private final ImageAiProvider provider;

    public ImageAiService(ImageAiProvider provider) {
        this.provider = provider;
    }

    public ImageAiResponseDTO generateImage(ImageAiRequestDTO request) {
        String nome = BrValidations.trimToNull(request.nome());
        if (nome == null) {
            throw new IllegalArgumentException("Provide the item name to generate the image.");
        }

        return provider.generateImage(new ImageAiRequestDTO(
                nome,
                BrValidations.trimToNull(request.category()),
                BrValidations.trimToNull(request.descricao())
        ));
    }
}

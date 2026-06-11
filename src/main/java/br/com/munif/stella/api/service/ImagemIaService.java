package br.com.munif.stella.api.service;

import br.com.munif.comum.utils.validacoes.ValidacoesBR;
import br.com.munif.stella.api.dto.ImagemIaRequestDTO;
import br.com.munif.stella.api.dto.ImagemIaResponseDTO;
import org.springframework.stereotype.Service;

@Service
public class ImagemIaService {

    private final ImagemIaProvider provider;

    public ImagemIaService(ImagemIaProvider provider) {
        this.provider = provider;
    }

    public ImagemIaResponseDTO gerarImagem(ImagemIaRequestDTO request) {
        String nome = ValidacoesBR.trimToNull(request.nome());
        if (nome == null) {
            throw new IllegalArgumentException("Informe o nome do item para gerar a imagem.");
        }

        return provider.gerarImagem(new ImagemIaRequestDTO(
                nome,
                ValidacoesBR.trimToNull(request.categoria()),
                ValidacoesBR.trimToNull(request.descricao())
        ));
    }
}

package br.com.munif.stella.api.service;

public enum AiOperation {
    IMAGE_ANALYSIS("análise de imagens"),
    IMAGE_GENERATION("geração de imagens"),
    EMBEDDING("geração de embeddings");

    private final String descricao;

    AiOperation(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}

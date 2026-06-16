package br.com.stella.api.service;

public enum AiOperation {
    IMAGE_ANALYSIS("image analysis"),
    IMAGE_GENERATION("image generation"),
    EMBEDDING("embedding generation");

    private final String descricao;

    AiOperation(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}

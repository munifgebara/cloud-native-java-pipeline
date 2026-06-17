package br.com.stella.api.service;

public enum AiOperation {
    IMAGE_ANALYSIS("image analysis"),
    IMAGE_GENERATION("image generation"),
    EMBEDDING("embedding generation");

    private final String description;

    AiOperation(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}

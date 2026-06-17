package br.com.stella.api.service;

public interface EmbeddingProvider {

    float[] generateEmbedding(String texto);
}

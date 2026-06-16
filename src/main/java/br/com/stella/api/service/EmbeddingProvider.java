package br.com.stella.api.service;

public interface EmbeddingProvider {

    float[] gerarEmbedding(String texto);
}

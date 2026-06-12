package br.com.munif.stella.api.service;

public interface EmbeddingProvider {

    float[] gerarEmbedding(String texto);
}

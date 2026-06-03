package br.com.munif.stella.api.entity;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum CategoriaIcone {
    ELETRONICOS("eletronicos"),
    MOVEIS("moveis"),
    FERRAMENTAS("ferramentas"),
    LIVROS("livros"),
    ROUPAS("roupas"),
    COZINHA("cozinha"),
    ESPORTES("esportes"),
    DOCUMENTOS("documentos"),
    OUTROS("outros");

    private static final Set<String> CHAVES = Arrays.stream(values())
            .map(CategoriaIcone::getChave)
            .collect(Collectors.toUnmodifiableSet());

    private final String chave;

    CategoriaIcone(String chave) {
        this.chave = chave;
    }

    public String getChave() {
        return chave;
    }

    public static boolean isChaveValida(String chave) {
        return chave == null || CHAVES.contains(chave);
    }
}

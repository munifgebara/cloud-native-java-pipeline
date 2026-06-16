package br.com.munif.stella.api.entity;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enumeration of available icons for visual identification of item categories.
 *
 * <p>Each value corresponds to an icon identifier used by the frontend interface.
 * The set of valid keys is pre-computed in {@link #CHAVES} for efficient validation
 * without the need for linear iteration.</p>
 *
 * <p>Typical usage: the {@code icone} field of {@link Categoria} stores the {@link #getChave() key} of the enum
 * as a string, and the method {@link #isChaveValida(String)} validates the value received via API.</p>
 */
public enum CategoriaIcone {

    /** Icon for electronic items (computers, phones, IT equipment). */
    ELETRONICOS("eletronicos"),

    /** Icon for furniture and furnishing items (chairs, tables, cabinets). */
    MOVEIS("moveis"),

    /** Icon for hand tools and maintenance equipment. */
    FERRAMENTAS("ferramentas"),

    /** Icon for books, publications, and bibliographic materials. */
    LIVROS("livros"),

    /** Icon for clothing, uniforms, and apparel. */
    ROUPAS("roupas"),

    /** Icon for kitchen utensils and pantry equipment. */
    COZINHA("cozinha"),

    /** Icon for sports and leisure equipment. */
    ESPORTES("esportes"),

    /** Icon for documents, files, and printed materials. */
    DOCUMENTOS("documentos"),

    /** Generic icon for items that do not fit into the other categories. */
    OUTROS("outros");

    /**
     * Immutable set of all valid keys, pre-computed at class initialization.
     * Used by {@link #isChaveValida(String)} for constant-time validation.
     */
    private static final Set<String> CHAVES = Arrays.stream(values())
            .map(CategoriaIcone::getChave)
            .collect(Collectors.toUnmodifiableSet());

    /** String identifier of the icon, as expected by the frontend. */
    private final String chave;

    /**
     * Enum constructor.
     *
     * @param chave string identifier of the icon
     */
    CategoriaIcone(String chave) {
        this.chave = chave;
    }

    /**
     * Returns the string identifier of this icon, as used by the frontend interface.
     *
     * @return the icon key (e.g.: {@code "eletronicos"})
     */
    public String getChave() {
        return chave;
    }

    /**
     * Checks whether the provided key corresponds to a valid icon.
     *
     * <p>Returns {@code true} also for {@code null}, allowing the field to be optional
     * in entities and DTOs without triggering validation errors.</p>
     *
     * @param chave the icon identifier to check; may be {@code null}
     * @return {@code true} if the key is {@code null} or among the registered values
     */
    public static boolean isChaveValida(String chave) {
        return chave == null || CHAVES.contains(chave);
    }
}

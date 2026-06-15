package br.com.munif.stella.api.entity;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enumeração dos ícones disponíveis para identificação visual de categorias de itens.
 *
 * <p>Cada valor corresponde a um identificador de ícone utilizado pela interface frontend.
 * O conjunto de chaves válidas é pré-computado em {@link #CHAVES} para validações eficientes
 * sem necessidade de iteração linear.</p>
 *
 * <p>Uso típico: o campo {@link Categoria#getIcone()} armazena a {@link #chave} do enum
 * como string, e o método {@link #isChaveValida(String)} valida o valor recebido via API.</p>
 */
public enum CategoriaIcone {

    /** Ícone para itens eletrônicos (computadores, celulares, equipamentos de TI). */
    ELETRONICOS("eletronicos"),

    /** Ícone para móveis e itens de mobiliário (cadeiras, mesas, armários). */
    MOVEIS("moveis"),

    /** Ícone para ferramentas manuais e equipamentos de manutenção. */
    FERRAMENTAS("ferramentas"),

    /** Ícone para livros, publicações e material bibliográfico. */
    LIVROS("livros"),

    /** Ícone para roupas, uniformes e indumentárias. */
    ROUPAS("roupas"),

    /** Ícone para utensílios de cozinha e equipamentos de copa. */
    COZINHA("cozinha"),

    /** Ícone para equipamentos esportivos e de lazer. */
    ESPORTES("esportes"),

    /** Ícone para documentos, arquivos e material impresso. */
    DOCUMENTOS("documentos"),

    /** Ícone genérico para itens que não se encaixam nas demais categorias. */
    OUTROS("outros");

    /**
     * Conjunto imutável de todas as chaves válidas, pré-computado na inicialização da classe.
     * Utilizado por {@link #isChaveValida(String)} para validação em tempo constante.
     */
    private static final Set<String> CHAVES = Arrays.stream(values())
            .map(CategoriaIcone::getChave)
            .collect(Collectors.toUnmodifiableSet());

    /** Identificador string do ícone, conforme esperado pelo frontend. */
    private final String chave;

    /**
     * Construtor do enum.
     *
     * @param chave identificador string do ícone
     */
    CategoriaIcone(String chave) {
        this.chave = chave;
    }

    /**
     * Retorna o identificador string deste ícone, conforme utilizado pela interface frontend.
     *
     * @return a chave do ícone (ex.: {@code "eletronicos"})
     */
    public String getChave() {
        return chave;
    }

    /**
     * Verifica se a chave informada corresponde a um ícone válido.
     *
     * <p>Retorna {@code true} também para {@code null}, permitindo que o campo seja opcional
     * nas entidades e DTOs sem disparar erros de validação.</p>
     *
     * @param chave o identificador de ícone a verificar; pode ser {@code null}
     * @return {@code true} se a chave for {@code null} ou estiver entre os valores cadastrados
     */
    public static boolean isChaveValida(String chave) {
        return chave == null || CHAVES.contains(chave);
    }
}

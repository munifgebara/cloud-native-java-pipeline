package br.com.munif.stella.api.entity;

import br.com.munif.comum.persistencia.Entidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * Entidade que representa uma categoria de itens do inventário.
 *
 * <p>Categorias organizam os {@link ItemMestre itens mestres} em grupos temáticos
 * (ex.: Eletrônicos, Móveis, Ferramentas), facilitando a busca e o filtro no sistema.
 * Cada categoria pode ter um ícone associado para identificação visual na interface.</p>
 *
 * <p>A entidade é auditada pelo Hibernate Envers: todas as alterações são registradas
 * na tabela {@code categoria_aud}.</p>
 */
@Entity
@Audited
@Table(name = "categoria")
@Getter
@Setter
@NoArgsConstructor
public class Categoria extends Entidade {

    /**
     * Nome de exibição da categoria.
     * Obrigatório, com até 150 caracteres.
     */
    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    /**
     * Texto descritivo opcional que detalha o propósito ou escopo da categoria.
     * Permite até 500 caracteres.
     */
    @Column(name = "descricao", length = 500)
    private String descricao;

    /**
     * Identificador do ícone visual associado à categoria (ex.: {@code "eletronicos"}, {@code "moveis"}).
     * Os valores válidos são definidos por {@link CategoriaIcone}.
     * Permite até 50 caracteres.
     */
    @Column(name = "icone", length = 50)
    private String icone;
}

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
 * Entity representing an inventory item category.
 *
 * <p>Categories organize {@link ItemMestre main items} into thematic groups
 * (e.g.: Electronics, Furniture, Tools), making search and filtering easier in the system.
 * Each category can have an associated icon for visual identification in the UI.</p>
 *
 * <p>This entity is audited by Hibernate Envers: all changes are recorded
 * in the {@code categoria_aud} table.</p>
 */
@Entity
@Audited
@Table(name = "categoria")
@Getter
@Setter
@NoArgsConstructor
public class Categoria extends Entidade {

    /**
     * Display name of the category.
     * Required, up to 150 characters.
     */
    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    /**
     * Optional descriptive text that details the purpose or scope of the category.
     * Up to 500 characters.
     */
    @Column(name = "descricao", length = 500)
    private String descricao;

    /**
     * Identifier of the visual icon associated with the category (e.g.: {@code "eletronicos"}, {@code "moveis"}).
     * Valid values are defined by {@link CategoriaIcone}.
     * Up to 50 characters.
     */
    @Column(name = "icone", length = 50)
    private String icone;
}

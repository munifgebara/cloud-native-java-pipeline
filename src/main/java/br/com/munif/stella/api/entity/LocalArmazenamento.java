package br.com.munif.stella.api.entity;

import br.com.munif.comum.persistencia.Entidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * Entity representing a storage location for inventory items.
 *
 * <p>Storage locations form a hierarchical structure: a location can have a
 * parent location ({@link #pai}), allowing buildings, floors, rooms and cabinets
 * to be represented in a nested fashion. For example: "Building A" &gt; "Room 101" &gt; "Cabinet 3".</p>
 *
 * <p>Each location can have an associated image, stored in an object storage service,
 * to facilitate visual identification by users.</p>
 *
 * <p>This entity is audited by Hibernate Envers: all changes are recorded
 * in the {@code local_armazenamento_aud} table.</p>
 */
@Entity
@Audited
@Table(name = "local_armazenamento")
@Getter
@Setter
@NoArgsConstructor
public class LocalArmazenamento extends Entidade {

    /**
     * Name of the storage location. Required, up to 150 characters.
     * Examples: {@code "Main Warehouse"}, {@code "IT Room"}, {@code "Cabinet 02"}.
     */
    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    /**
     * Optional descriptive text with additional information about the location
     * (e.g.: capacity, type of stored items, access restrictions).
     * Up to 500 characters.
     */
    @Column(name = "descricao", length = 500)
    private String descricao;

    /**
     * Name of the bucket in the object storage service where the location's image is stored.
     * {@code null} when the location has no registered image.
     * Up to 100 characters.
     */
    @Column(name = "imagem_bucket", length = 100)
    private String imagemBucket;

    /**
     * Key (path/object name) of the image within the object storage bucket.
     * Used together with {@link #imagemBucket} to locate the file.
     * Up to 500 characters.
     */
    @Column(name = "imagem_object_key", length = 500)
    private String imagemObjectKey;

    /**
     * MIME type of the stored image (e.g.: {@code "image/jpeg"}, {@code "image/png"}).
     * Required to serve the file with the correct {@code Content-Type} header.
     * Up to 100 characters.
     */
    @Column(name = "imagem_content_type", length = 100)
    private String imagemContentType;

    /**
     * Size of the image in bytes. Useful for displaying information to the user
     * and for storage quota checks.
     */
    @Column(name = "imagem_tamanho_bytes")
    private Long imagemTamanhoBytes;

    /**
     * Parent location in the storage hierarchy.
     * {@code null} indicates this is a root location (top level of the hierarchy).
     * Loaded lazily to avoid unnecessary chained joins.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_pai_id")
    private LocalArmazenamento pai;
}

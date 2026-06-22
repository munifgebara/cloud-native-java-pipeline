package br.com.stella.api.entity;

import br.com.munif.common.persistencia.BaseEntity;
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
 * parent location ({@link #parent}), allowing buildings, floors, rooms and cabinets
 * to be represented in a nested fashion. For example: "Building A" &gt; "Room 101" &gt; "Cabinet 3".</p>
 *
 * <p>Each location can have an associated image, stored in an object storage service,
 * to facilitate visual identification by users.</p>
 *
 * <p>This entity is audited by Hibernate Envers: all changes are recorded
 * in the {@code storage_location_aud} table.</p>
 */
@Entity
@Audited
@Table(name = "storage_location")
@Getter
@Setter
@NoArgsConstructor
public class StorageLocation extends BaseEntity {

    /**
     * Name of the storage location. Required, up to 150 characters.
     * Examples: {@code "Main Warehouse"}, {@code "IT Room"}, {@code "Cabinet 02"}.
     */
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * Optional descriptive text with additional information about the location
     * (e.g.: capacity, type of stored items, access restrictions).
     * Up to 500 characters.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Name of the bucket in the object storage service where the location's image is stored.
     * {@code null} when the location has no registered image.
     * Up to 100 characters.
     */
    @Column(name = "image_bucket", length = 100)
    private String imageBucket;

    /**
     * Key (path/object name) of the image within the object storage bucket.
     * Used together with {@link #imageBucket} to locate the file.
     * Up to 500 characters.
     */
    @Column(name = "image_object_key", length = 500)
    private String imageObjectKey;

    /**
     * MIME type of the stored image (e.g.: {@code "image/jpeg"}, {@code "image/png"}).
     * Required to serve the file with the correct {@code Content-Type} header.
     * Up to 100 characters.
     */
    @Column(name = "image_content_type", length = 100)
    private String imageContentType;

    /**
     * Size of the image in bytes. Useful for displaying information to the user
     * and for storage quota checks.
     */
    @Column(name = "image_size_bytes")
    private Long imageSizeBytes;

    /**
     * Parent location in the storage hierarchy.
     * {@code null} indicates this is a root location (top level of the hierarchy).
     * Loaded lazily to avoid unnecessary chained joins.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_location_id")
    private StorageLocation parent;
}

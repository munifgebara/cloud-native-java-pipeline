package br.com.stella.api.entity;

import br.com.munif.common.persistencia.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * Entity representing an individual or legal entity in the system.
 *
 * <p>People are primarily used as borrowers in item loans from the inventory.
 * The {@link #taxId} document is unique in the system and identifies whether the entry
 * is an individual (CPF, 11 digits) or a legal entity (CNPJ, 14 digits).</p>
 *
 * <p>The full address is stored in separate fields to facilitate integrations
 * with postal code lookup and geolocation services.</p>
 *
 * <p>This entity is audited by Hibernate Envers: all changes are recorded
 * in the {@code pessoa_aud} table.</p>
 */
@Entity
@Audited
@Table(name = "pessoa")

@Getter
@Setter
@NoArgsConstructor
public class Person extends BaseEntity {

    /**
     * Full name of the individual or business name of the legal entity.
     * Required, up to 150 characters.
     */
    @Column(name = "nome", nullable = false, length = 150)
    private String name;

    /**
     * CPF (11 digits) or CNPJ (14 digits) of the person, without formatting (digits only).
     * Must be unique in the system — used as the natural identification key.
     */
    @Column(name = "cpf_cnpj", nullable = false, length = 14, unique = true)
    private String taxId;

    /**
     * Primary contact phone number (mobile or landline).
     * Free format, up to 20 characters (e.g.: {@code "(11) 98765-4321"}).
     */
    @Column(name = "telefone_principal", length = 20)
    private String primaryPhone;

    /**
     * Alternative contact phone number.
     * Free format, up to 20 characters.
     */
    @Column(name = "telefone_secundario", length = 20)
    private String secondaryPhone;

    /**
     * Email address of the person for communications and notifications.
     * Up to 150 characters.
     */
    @Column(name = "email", length = 150)
    private String email;

    /**
     * Brazilian postal code (CEP) of the address, digits only (8 characters).
     * Example: {@code "01310100"} for Av. Paulista in São Paulo.
     */
    @Column(name = "cep", length = 8)
    private String zipCode;

    /**
     * Street address (road, avenue, etc.) including the number.
     * Up to 200 characters.
     */
    @Column(name = "endereco", length = 200)
    private String address;

    /**
     * Address complement (e.g.: apartment, block, unit).
     * Up to 100 characters.
     */
    @Column(name = "complemento", length = 100)
    private String complement;

    /**
     * Neighborhood of the address.
     * Up to 100 characters.
     */
    @Column(name = "bairro", length = 100)
    private String neighborhood;

    /**
     * City of the address.
     * Up to 100 characters.
     */
    @Column(name = "cidade", length = 100)
    private String city;

    /**
     * State abbreviation (Brazilian Federative Unit) of the address, 2 uppercase letters.
     * Example: {@code "SP"}, {@code "RJ"}, {@code "MG"}.
     */
    @Column(name = "uf", length = 2)
    private String state;

    @Column(name = "foto_bucket", length = 100)
    private String photoBucket;

    @Column(name = "foto_object_key", length = 500)
    private String photoObjectKey;

    @Column(name = "foto_content_type", length = 100)
    private String photoContentType;

    @Column(name = "foto_tamanho_bytes")
    private Long photoSizeBytes;
}

package br.com.stella.api.mapper;

import br.com.stella.api.dto.PersonCreateDTO;
import br.com.stella.api.dto.PersonResponseDTO;
import br.com.stella.api.dto.PersonSummaryDTO;
import br.com.stella.api.dto.PersonUpdateDTO;
import br.com.stella.api.entity.Person;


/**
 * Converts between the {@link Person} entity and its input and output DTOs.
 *
 * <p>Static utility class — must not be instantiated.
 * Centralizes all mapping logic for {@code Person},
 * avoiding duplication in services and controllers.</p>
 */
public final class PersonMapper {

    private PersonMapper() {
    }

    /**
     * Creates a new {@link Person} entity from creation data.
     *
     * @param dto person creation data; may be {@code null}
     * @return new populated {@link Person} instance, or {@code null} if {@code dto} is {@code null}
     */
    public static Person toEntity(PersonCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        Person person = new Person();
        person.setName(dto.name());
        person.setTaxId(dto.cpfCnpj());
        person.setPrimaryPhone(dto.telefonePrincipal());
        person.setSecondaryPhone(dto.telefoneSecundario());
        person.setEmail(dto.email());
        person.setZipCode(dto.cep());
        person.setAddress(dto.endereco());
        person.setComplement(dto.complemento());
        person.setNeighborhood(dto.bairro());
        person.setCity(dto.cidade());
        person.setState(dto.uf());
        return person;
    }

    /**
     * Applies update data onto an existing {@link Person} entity.
     *
     * <p>The CPF/CNPJ is not updated by this method — it is immutable after registration.</p>
     *
     * @param entity entity to be updated; ignored if {@code null}
     * @param dto    update data; ignored if {@code null}
     */
    public static void updateEntity(Person entity, PersonUpdateDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setName(dto.name());
        entity.setPrimaryPhone(dto.telefonePrincipal());
        entity.setSecondaryPhone(dto.telefoneSecundario());
        entity.setEmail(dto.email());
        entity.setZipCode(dto.cep());
        entity.setAddress(dto.endereco());
        entity.setComplement(dto.complemento());
        entity.setNeighborhood(dto.bairro());
        entity.setCity(dto.cidade());
        entity.setState(dto.uf());
    }

    /**
     * Converts the {@link Person} entity to the full response DTO.
     *
     * @param entity entity to convert; may be {@code null}
     * @return populated {@link PersonResponseDTO}, or {@code null} if {@code entity} is {@code null}
     */
    public static PersonResponseDTO toResponseDTO(Person entity) {
        if (entity == null) {
            return null;
        }

        return new PersonResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getTaxId(),
                entity.getPrimaryPhone(),
                entity.getSecondaryPhone(),
                entity.getEmail(),
                entity.getZipCode(),
                entity.getAddress(),
                entity.getComplement(),
                entity.getNeighborhood(),
                entity.getCity(),
                entity.getState(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Converts the {@link Person} entity to the summary DTO used in listings and selectors.
     *
     * @param entity entity to convert; may be {@code null}
     * @return populated {@link PersonSummaryDTO}, or {@code null} if {@code entity} is {@code null}
     */
    public static PersonSummaryDTO toResumoDTO(Person entity) {
        if (entity == null) {
            return null;
        }

        return new PersonSummaryDTO(
                entity.getId(),
                entity.getName()
        );
    }
}

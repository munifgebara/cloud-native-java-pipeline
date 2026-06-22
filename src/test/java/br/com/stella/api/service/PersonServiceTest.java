package br.com.stella.api.service;

import br.com.stella.api.dto.MainItemImageDTO;
import br.com.stella.api.dto.PersonCreateDTO;
import br.com.stella.api.entity.Person;
import br.com.stella.api.exception.DuplicateRegistrationException;
import br.com.stella.api.repository.PersonRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PessoaServiceTest {

    @Mock
    private PersonRepository repository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private MainItemImageStorageService imageStorageService;

    @InjectMocks
    private PersonService service;

    @Test
    void shouldCreatePersonNormalizingFields() {
        Instant createdAt = Instant.parse("2026-06-07T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-06-07T10:30:00Z");

        when(repository.existsByTaxId("52998224725")).thenReturn(false);
        when(repository.save(any(Person.class))).thenAnswer(invocation -> {
            Person person = invocation.getArgument(0);
            person.setCreatedAt(createdAt);
            person.setUpdatedAt(updatedAt);
            return person;
        });

        var response = service.create(new PersonCreateDTO(
                "  Maria Silva  ",
                "529.982.247-25",
                " (11) 99999-9999 ",
                null,
                "  MARIA@EXEMPLO.COM  ",
                "01310-100",
                "  Avenida Paulista  ",
                "  Conjunto 10  ",
                "  Bela Vista  ",
                "  Sao Paulo  ",
                " sp "
        ));

        ArgumentCaptor<Person> captor = ArgumentCaptor.forClass(Person.class);
        verify(repository).save(captor.capture());

        Person pessoaSalva = captor.getValue();
        assertThat(pessoaSalva.getName()).isEqualTo("Maria Silva");
        assertThat(pessoaSalva.getTaxId()).isEqualTo("52998224725");
        assertThat(pessoaSalva.getPrimaryPhone()).isEqualTo("11999999999");
        assertThat(pessoaSalva.getEmail()).isEqualTo("maria@exemplo.com");
        assertThat(pessoaSalva.getZipCode()).isEqualTo("01310100");
        assertThat(pessoaSalva.getAddress()).isEqualTo("Avenida Paulista");
        assertThat(pessoaSalva.getComplement()).isEqualTo("Conjunto 10");
        assertThat(pessoaSalva.getNeighborhood()).isEqualTo("Bela Vista");
        assertThat(pessoaSalva.getCity()).isEqualTo("Sao Paulo");
        assertThat(pessoaSalva.getState()).isEqualTo("SP");
        assertThat(response.taxId()).isEqualTo("52998224725");
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldPreventPersonWithCpfCnpjDuplicate() {
        when(repository.existsByTaxId("52998224725")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new PersonCreateDTO(
                "Maria Silva",
                "529.982.247-25",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        )))
                .isInstanceOf(DuplicateRegistrationException.class)
                .hasMessageContaining("CPF/CNPJ");

        verify(repository, never()).save(any(Person.class));
    }

    @Test
    void shouldFindByNameOnlyWhenFilterProvided() {
        Person person = new Person();
        person.setId(UUID.randomUUID());
        person.setName("Maria Silva");

        when(repository.findByActiveTrueAndNameContainingIgnoreCase("Maria")).thenReturn(List.of(person));

        assertThat(service.findByName("  ")).isEmpty();
        assertThat(service.findByName(" Maria ")).hasSize(1);
    }

    @Test
    void shouldUpdatePersonPhotoReplacingPreviousObject() {
        UUID id = UUID.randomUUID();
        Person person = new Person();
        person.setId(id);
        person.setName("Maria Silva");
        person.setPhotoBucket("bucket-antigo");
        person.setPhotoObjectKey("people/%s/antiga.png".formatted(id));

        var file = new MockMultipartFile("file", "nova.png", "image/png", new byte[]{1, 2});
        var image = new MainItemImageDTO("bucket-novo", "people/%s/nova.png".formatted(id), "image/png", 2L);

        when(repository.findById(id)).thenReturn(Optional.of(person));
        when(imageStorageService.storePersonPhoto(id, file)).thenReturn(image);
        when(repository.save(person)).thenReturn(person);

        var response = service.updatePhoto(id, file);

        assertThat(person.getPhotoBucket()).isEqualTo("bucket-novo");
        assertThat(person.getPhotoObjectKey()).isEqualTo("people/%s/nova.png".formatted(id));
        assertThat(person.getPhotoContentType()).isEqualTo("image/png");
        assertThat(person.getPhotoSizeBytes()).isEqualTo(2L);
        assertThat(response.photoUrl()).isEqualTo("/api/public/people/%s/photo".formatted(id));
        verify(imageStorageService).removeSilently("bucket-antigo", "people/%s/antiga.png".formatted(id));
    }

    @Test
    void shouldRemovePersonPhoto() {
        UUID id = UUID.randomUUID();
        Person person = new Person();
        person.setId(id);
        person.setName("Maria Silva");
        person.setPhotoBucket("bucket");
        person.setPhotoObjectKey("people/%s/photo.png".formatted(id));
        person.setPhotoContentType("image/png");
        person.setPhotoSizeBytes(2L);

        when(repository.findById(id)).thenReturn(Optional.of(person));
        when(repository.save(person)).thenReturn(person);

        var response = service.removePhoto(id);

        assertThat(person.getPhotoBucket()).isNull();
        assertThat(person.getPhotoObjectKey()).isNull();
        assertThat(person.getPhotoContentType()).isNull();
        assertThat(person.getPhotoSizeBytes()).isNull();
        assertThat(response.photoUrl()).isNull();
        verify(imageStorageService).removeSilently("bucket", "people/%s/photo.png".formatted(id));
    }
}

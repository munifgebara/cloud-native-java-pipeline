package br.com.stella.api.service;

import br.com.stella.api.dto.PersonCreateDTO;
import br.com.stella.api.entity.Person;
import br.com.stella.api.exception.DuplicateRegistrationException;
import br.com.stella.api.repository.PersonRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
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

    @InjectMocks
    private PersonService service;

    @Test
    void deveCriarPessoaNormalizandoCampos() {
        Instant createdAt = Instant.parse("2026-06-07T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-06-07T10:30:00Z");

        when(repository.existsByTaxId("52998224725")).thenReturn(false);
        when(repository.save(any(Person.class))).thenAnswer(invocation -> {
            Person pessoa = invocation.getArgument(0);
            pessoa.setCreatedAt(createdAt);
            pessoa.setUpdatedAt(updatedAt);
            return pessoa;
        });

        var resposta = service.criar(new PersonCreateDTO(
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
        assertThat(resposta.cpfCnpj()).isEqualTo("52998224725");
        assertThat(resposta.createdAt()).isEqualTo(createdAt);
        assertThat(resposta.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void deveImpedirPessoaComCpfCnpjDuplicado() {
        when(repository.existsByTaxId("52998224725")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(new PersonCreateDTO(
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
    void deveBuscarPorNomeSomenteQuandoFiltroInformado() {
        Person pessoa = new Person();
        pessoa.setId(UUID.randomUUID());
        pessoa.setName("Maria Silva");

        when(repository.findByActiveTrueAndNameContainingIgnoreCase("Maria")).thenReturn(List.of(pessoa));

        assertThat(service.buscarPorNome("  ")).isEmpty();
        assertThat(service.buscarPorNome(" Maria ")).hasSize(1);
    }
}

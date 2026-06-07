package br.com.munif.stella.api.service;

import br.com.munif.stella.api.dto.PessoaCreateDTO;
import br.com.munif.stella.api.entity.Pessoa;
import br.com.munif.stella.api.exception.CadastroDuplicadoException;
import br.com.munif.stella.api.repository.PessoaRepository;
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
    private PessoaRepository repository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private PessoaService service;

    @Test
    void deveCriarPessoaNormalizandoCampos() {
        Instant criadoEm = Instant.parse("2026-06-07T10:00:00Z");
        Instant alteradoEm = Instant.parse("2026-06-07T10:30:00Z");

        when(repository.existsByCpfCnpj("52998224725")).thenReturn(false);
        when(repository.save(any(Pessoa.class))).thenAnswer(invocation -> {
            Pessoa pessoa = invocation.getArgument(0);
            pessoa.setCriadoEm(criadoEm);
            pessoa.setAlteradoEm(alteradoEm);
            return pessoa;
        });

        var resposta = service.criar(new PessoaCreateDTO(
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

        ArgumentCaptor<Pessoa> captor = ArgumentCaptor.forClass(Pessoa.class);
        verify(repository).save(captor.capture());

        Pessoa pessoaSalva = captor.getValue();
        assertThat(pessoaSalva.getNome()).isEqualTo("Maria Silva");
        assertThat(pessoaSalva.getCpfCnpj()).isEqualTo("52998224725");
        assertThat(pessoaSalva.getTelefonePrincipal()).isEqualTo("11999999999");
        assertThat(pessoaSalva.getEmail()).isEqualTo("maria@exemplo.com");
        assertThat(pessoaSalva.getCep()).isEqualTo("01310100");
        assertThat(pessoaSalva.getEndereco()).isEqualTo("Avenida Paulista");
        assertThat(pessoaSalva.getComplemento()).isEqualTo("Conjunto 10");
        assertThat(pessoaSalva.getBairro()).isEqualTo("Bela Vista");
        assertThat(pessoaSalva.getCidade()).isEqualTo("Sao Paulo");
        assertThat(pessoaSalva.getUf()).isEqualTo("SP");
        assertThat(resposta.cpfCnpj()).isEqualTo("52998224725");
        assertThat(resposta.criadoEm()).isEqualTo(criadoEm);
        assertThat(resposta.alteradoEm()).isEqualTo(alteradoEm);
    }

    @Test
    void deveImpedirPessoaComCpfCnpjDuplicado() {
        when(repository.existsByCpfCnpj("52998224725")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(new PessoaCreateDTO(
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
                .isInstanceOf(CadastroDuplicadoException.class)
                .hasMessageContaining("CPF/CNPJ");

        verify(repository, never()).save(any(Pessoa.class));
    }

    @Test
    void deveBuscarPorNomeSomenteQuandoFiltroInformado() {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(UUID.randomUUID());
        pessoa.setNome("Maria Silva");

        when(repository.findByAtivoTrueAndNomeContainingIgnoreCase("Maria")).thenReturn(List.of(pessoa));

        assertThat(service.buscarPorNome("  ")).isEmpty();
        assertThat(service.buscarPorNome(" Maria ")).hasSize(1);
    }
}

package br.com.munif.stella.api.service;

import br.com.munif.stella.api.dto.EmprestimoItemCreateDTO;
import br.com.munif.stella.api.entity.EmprestimoItem;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.ItemMestre;
import br.com.munif.stella.api.entity.LocalArmazenamento;
import br.com.munif.stella.api.entity.Pessoa;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import br.com.munif.stella.api.repository.EmprestimoItemRepository;
import br.com.munif.stella.api.repository.InstanciaItemRepository;
import br.com.munif.stella.api.repository.PessoaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmprestimoItemServiceTest {

    @Mock
    private EmprestimoItemRepository repository;

    @Mock
    private InstanciaItemRepository instanciaItemRepository;

    @Mock
    private PessoaRepository pessoaRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private EmprestimoItemService service;

    @Test
    void deveRegistrarEmprestimoAtualizandoStatusDaInstancia() {
        UUID instanciaId = UUID.randomUUID();
        UUID pessoaId = UUID.randomUUID();
        InstanciaItem instancia = instancia(instanciaId, StatusOperacionalInstancia.DISPONIVEL, true, local(UUID.randomUUID(), "Biblioteca"));
        Pessoa pessoa = pessoa(pessoaId, "Maria Silva", true);
        LocalDate previsao = LocalDate.now().plusDays(7);

        when(instanciaItemRepository.findById(instanciaId)).thenReturn(Optional.of(instancia));
        when(pessoaRepository.findById(pessoaId)).thenReturn(Optional.of(pessoa));
        when(repository.existsByInstanciaItemIdAndDataDevolucaoIsNull(instanciaId)).thenReturn(false);
        when(instanciaItemRepository.save(any(InstanciaItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(EmprestimoItem.class))).thenAnswer(invocation -> {
            EmprestimoItem emprestimo = invocation.getArgument(0);
            emprestimo.setId(UUID.randomUUID());
            return emprestimo;
        });

        var resposta = service.registrarEmprestimo(new EmprestimoItemCreateDTO(
                instanciaId,
                pessoaId,
                previsao,
                "  Retirado para uso em aula  "
        ));

        ArgumentCaptor<InstanciaItem> instanciaCaptor = ArgumentCaptor.forClass(InstanciaItem.class);
        ArgumentCaptor<EmprestimoItem> emprestimoCaptor = ArgumentCaptor.forClass(EmprestimoItem.class);
        verify(instanciaItemRepository).save(instanciaCaptor.capture());
        verify(repository).save(emprestimoCaptor.capture());

        InstanciaItem instanciaAtualizada = instanciaCaptor.getValue();
        assertThat(instanciaAtualizada.getStatusOperacional()).isEqualTo(StatusOperacionalInstancia.EMPRESTADO);
        assertThat(instanciaAtualizada.getLocalAtual()).isNull();

        EmprestimoItem emprestimo = emprestimoCaptor.getValue();
        assertThat(emprestimo.getInstanciaItem()).isEqualTo(instancia);
        assertThat(emprestimo.getPessoa()).isEqualTo(pessoa);
        assertThat(emprestimo.getPrevisaoDevolucao()).isEqualTo(previsao);
        assertThat(emprestimo.getObservacao()).isEqualTo("Retirado para uso em aula");
        assertThat(resposta.pessoaId()).isEqualTo(pessoaId);
        assertThat(resposta.instanciaItemId()).isEqualTo(instanciaId);
    }

    @Test
    void deveImpedirEmprestimoDeInstanciaIndisponivel() {
        UUID instanciaId = UUID.randomUUID();
        UUID pessoaId = UUID.randomUUID();
        InstanciaItem instancia = instancia(instanciaId, StatusOperacionalInstancia.EM_MOVIMENTACAO, true, local(UUID.randomUUID(), "Biblioteca"));

        when(instanciaItemRepository.findById(instanciaId)).thenReturn(Optional.of(instancia));
        when(pessoaRepository.findById(pessoaId)).thenReturn(Optional.of(pessoa(pessoaId, "Maria Silva", true)));

        assertThatThrownBy(() -> service.registrarEmprestimo(new EmprestimoItemCreateDTO(instanciaId, pessoaId, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disponíveis");

        verify(instanciaItemRepository, never()).save(any(InstanciaItem.class));
        verify(repository, never()).save(any(EmprestimoItem.class));
    }

    @Test
    void deveImpedirEmprestimoAbertoDuplicado() {
        UUID instanciaId = UUID.randomUUID();
        UUID pessoaId = UUID.randomUUID();
        InstanciaItem instancia = instancia(instanciaId, StatusOperacionalInstancia.DISPONIVEL, true, local(UUID.randomUUID(), "Biblioteca"));

        when(instanciaItemRepository.findById(instanciaId)).thenReturn(Optional.of(instancia));
        when(pessoaRepository.findById(pessoaId)).thenReturn(Optional.of(pessoa(pessoaId, "Maria Silva", true)));
        when(repository.existsByInstanciaItemIdAndDataDevolucaoIsNull(instanciaId)).thenReturn(true);

        assertThatThrownBy(() -> service.registrarEmprestimo(new EmprestimoItemCreateDTO(instanciaId, pessoaId, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empréstimo aberto");

        verify(instanciaItemRepository, never()).save(any(InstanciaItem.class));
        verify(repository, never()).save(any(EmprestimoItem.class));
    }

    private InstanciaItem instancia(UUID id, StatusOperacionalInstancia status, boolean ativa, LocalArmazenamento local) {
        InstanciaItem instancia = new InstanciaItem();
        instancia.setId(id);
        instancia.setItemMestre(new ItemMestre());
        instancia.setIdentificador("LIV-001");
        instancia.setStatusOperacional(status);
        instancia.setAtivo(ativa);
        instancia.setLocalAtual(local);
        return instancia;
    }

    private Pessoa pessoa(UUID id, String nome, boolean ativa) {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(id);
        pessoa.setNome(nome);
        pessoa.setAtivo(ativa);
        return pessoa;
    }

    private LocalArmazenamento local(UUID id, String nome) {
        LocalArmazenamento local = new LocalArmazenamento();
        local.setId(id);
        local.setNome(nome);
        local.setAtivo(true);
        return local;
    }
}

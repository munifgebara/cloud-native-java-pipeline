package br.com.stella.api.service;

import br.com.stella.api.dto.ItemLoanCreateDTO;
import br.com.stella.api.dto.ItemLoanReturnDTO;
import br.com.stella.api.entity.ItemLoan;
import br.com.stella.api.entity.ItemInstance;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.entity.StorageLocation;
import br.com.stella.api.entity.Person;
import br.com.stella.api.entity.ItemInstanceStatus;
import br.com.stella.api.repository.ItemLoanRepository;
import br.com.stella.api.repository.ItemInstanceRepository;
import br.com.stella.api.repository.StorageLocationRepository;
import br.com.stella.api.repository.PersonRepository;
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
    private ItemLoanRepository repository;

    @Mock
    private ItemInstanceRepository instanciaItemRepository;

    @Mock
    private PersonRepository pessoaRepository;

    @Mock
    private StorageLocationRepository localArmazenamentoRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ItemLoanService service;

    @Test
    void deveRegistrarEmprestimoAtualizandoStatusDaInstancia() {
        UUID instanciaId = UUID.randomUUID();
        UUID pessoaId = UUID.randomUUID();
        ItemInstance instance = instance(instanciaId, ItemInstanceStatus.DISPONIVEL, true, location(UUID.randomUUID(), "Biblioteca"));
        Person pessoa = pessoa(pessoaId, "Maria Silva", true);
        LocalDate previsao = LocalDate.now().plusDays(7);

        when(instanciaItemRepository.findById(instanciaId)).thenReturn(Optional.of(instance));
        when(pessoaRepository.findById(pessoaId)).thenReturn(Optional.of(pessoa));
        when(repository.existsByItemInstanceIdAndReturnDateIsNull(instanciaId)).thenReturn(false);
        when(instanciaItemRepository.save(any(ItemInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(ItemLoan.class))).thenAnswer(invocation -> {
            ItemLoan emprestimo = invocation.getArgument(0);
            emprestimo.setId(UUID.randomUUID());
            return emprestimo;
        });

        var resposta = service.registrarEmprestimo(new ItemLoanCreateDTO(
                instanciaId,
                pessoaId,
                previsao,
                "  Checked out for class use  "
        ));

        ArgumentCaptor<ItemInstance> instanciaCaptor = ArgumentCaptor.forClass(ItemInstance.class);
        ArgumentCaptor<ItemLoan> emprestimoCaptor = ArgumentCaptor.forClass(ItemLoan.class);
        verify(instanciaItemRepository).save(instanciaCaptor.capture());
        verify(repository).save(emprestimoCaptor.capture());

        ItemInstance instanciaAtualizada = instanciaCaptor.getValue();
        assertThat(instanciaAtualizada.getOperationalStatus()).isEqualTo(ItemInstanceStatus.EMPRESTADO);
        assertThat(instanciaAtualizada.getCurrentLocation()).isNull();

        ItemLoan emprestimo = emprestimoCaptor.getValue();
        assertThat(emprestimo.getItemInstance()).isEqualTo(instance);
        assertThat(emprestimo.getPerson()).isEqualTo(pessoa);
        assertThat(emprestimo.getExpectedReturnDate()).isEqualTo(previsao);
        assertThat(emprestimo.getNotes()).isEqualTo("Checked out for class use");
        assertThat(resposta.pessoaId()).isEqualTo(pessoaId);
        assertThat(resposta.instanciaItemId()).isEqualTo(instanciaId);
    }

    @Test
    void deveImpedirEmprestimoDeInstanciaIndisponivel() {
        UUID instanciaId = UUID.randomUUID();
        UUID pessoaId = UUID.randomUUID();
        ItemInstance instance = instance(instanciaId, ItemInstanceStatus.EM_MOVIMENTACAO, true, location(UUID.randomUUID(), "Biblioteca"));

        when(instanciaItemRepository.findById(instanciaId)).thenReturn(Optional.of(instance));
        when(pessoaRepository.findById(pessoaId)).thenReturn(Optional.of(pessoa(pessoaId, "Maria Silva", true)));

        assertThatThrownBy(() -> service.registrarEmprestimo(new ItemLoanCreateDTO(instanciaId, pessoaId, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("available instances can be loaned");

        verify(instanciaItemRepository, never()).save(any(ItemInstance.class));
        verify(repository, never()).save(any(ItemLoan.class));
    }

    @Test
    void deveImpedirEmprestimoAbertoDuplicado() {
        UUID instanciaId = UUID.randomUUID();
        UUID pessoaId = UUID.randomUUID();
        ItemInstance instance = instance(instanciaId, ItemInstanceStatus.DISPONIVEL, true, location(UUID.randomUUID(), "Biblioteca"));

        when(instanciaItemRepository.findById(instanciaId)).thenReturn(Optional.of(instance));
        when(pessoaRepository.findById(pessoaId)).thenReturn(Optional.of(pessoa(pessoaId, "Maria Silva", true)));
        when(repository.existsByItemInstanceIdAndReturnDateIsNull(instanciaId)).thenReturn(true);

        assertThatThrownBy(() -> service.registrarEmprestimo(new ItemLoanCreateDTO(instanciaId, pessoaId, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("open loan for this instance");

        verify(instanciaItemRepository, never()).save(any(ItemInstance.class));
        verify(repository, never()).save(any(ItemLoan.class));
    }

    @Test
    void deveRegistrarDevolucaoEncerrandoEmprestimoEAtualizandoInstancia() {
        UUID instanciaId = UUID.randomUUID();
        UUID localId = UUID.randomUUID();
        ItemInstance instance = instance(instanciaId, ItemInstanceStatus.EMPRESTADO, true, null);
        Person pessoa = pessoa(UUID.randomUUID(), "Maria Silva", true);
        StorageLocation localRetorno = location(localId, "Biblioteca");
        ItemLoan emprestimo = emprestimo(instance, pessoa);

        when(repository.findByItemInstanceIdAndReturnDateIsNull(instanciaId)).thenReturn(Optional.of(emprestimo));
        when(localArmazenamentoRepository.findById(localId)).thenReturn(Optional.of(localRetorno));
        when(instanciaItemRepository.save(any(ItemInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(ItemLoan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.registrarDevolucao(new ItemLoanReturnDTO(
                instanciaId,
                localId,
                "  Devolvido sem avarias  "
        ));

        ArgumentCaptor<ItemInstance> instanciaCaptor = ArgumentCaptor.forClass(ItemInstance.class);
        ArgumentCaptor<ItemLoan> emprestimoCaptor = ArgumentCaptor.forClass(ItemLoan.class);
        verify(instanciaItemRepository).save(instanciaCaptor.capture());
        verify(repository).save(emprestimoCaptor.capture());

        ItemInstance instanciaAtualizada = instanciaCaptor.getValue();
        assertThat(instanciaAtualizada.getOperationalStatus()).isEqualTo(ItemInstanceStatus.DISPONIVEL);
        assertThat(instanciaAtualizada.getCurrentLocation()).isEqualTo(localRetorno);

        ItemLoan emprestimoAtualizado = emprestimoCaptor.getValue();
        assertThat(emprestimoAtualizado.getReturnDate()).isNotNull();
        assertThat(emprestimoAtualizado.getNotes()).isEqualTo("Devolvido sem avarias");
        assertThat(resposta.dataDevolucao()).isNotNull();
        assertThat(resposta.instanciaItemId()).isEqualTo(instanciaId);
    }

    @Test
    void deveImpedirDevolucaoSemEmprestimoAberto() {
        UUID instanciaId = UUID.randomUUID();
        UUID localId = UUID.randomUUID();

        when(repository.findByItemInstanceIdAndReturnDateIsNull(instanciaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarDevolucao(new ItemLoanReturnDTO(instanciaId, localId, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("open loan for this instance");

        verify(instanciaItemRepository, never()).save(any(ItemInstance.class));
        verify(repository, never()).save(any(ItemLoan.class));
    }

    @Test
    void deveImpedirDevolucaoQuandoInstanciaNaoEstaEmprestada() {
        UUID instanciaId = UUID.randomUUID();
        UUID localId = UUID.randomUUID();
        ItemInstance instance = instance(instanciaId, ItemInstanceStatus.DISPONIVEL, true, null);
        ItemLoan emprestimo = emprestimo(instance, pessoa(UUID.randomUUID(), "Maria Silva", true));

        when(repository.findByItemInstanceIdAndReturnDateIsNull(instanciaId)).thenReturn(Optional.of(emprestimo));
        when(localArmazenamentoRepository.findById(localId)).thenReturn(Optional.of(location(localId, "Biblioteca")));

        assertThatThrownBy(() -> service.registrarDevolucao(new ItemLoanReturnDTO(instanciaId, localId, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be loaned");

        verify(instanciaItemRepository, never()).save(any(ItemInstance.class));
        verify(repository, never()).save(any(ItemLoan.class));
    }

    private ItemInstance instance(UUID id, ItemInstanceStatus status, boolean ativa, StorageLocation location) {
        ItemInstance instance = new ItemInstance();
        instance.setId(id);
        instance.setMainItem(new MainItem());
        instance.setIdentifier("LIV-001");
        instance.setOperationalStatus(status);
        instance.setActive(ativa);
        instance.setCurrentLocation(location);
        return instance;
    }

    private Person pessoa(UUID id, String nome, boolean ativa) {
        Person pessoa = new Person();
        pessoa.setId(id);
        pessoa.setName(nome);
        pessoa.setActive(ativa);
        return pessoa;
    }

    private StorageLocation location(UUID id, String nome) {
        StorageLocation location = new StorageLocation();
        location.setId(id);
        location.setName(nome);
        location.setActive(true);
        return location;
    }

    private ItemLoan emprestimo(ItemInstance instance, Person pessoa) {
        ItemLoan emprestimo = new ItemLoan();
        emprestimo.setId(UUID.randomUUID());
        emprestimo.setItemInstance(instance);
        emprestimo.setPerson(pessoa);
        emprestimo.setNotes("Loan inicial");
        return emprestimo;
    }
}

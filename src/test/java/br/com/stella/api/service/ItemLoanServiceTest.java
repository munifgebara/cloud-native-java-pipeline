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
    private ItemInstanceRepository itemInstanceRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private StorageLocationRepository storageLocationRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ItemLoanService service;

    @Test
    void shouldRegisterLoanUpdatingStatusOfInstance() {
        UUID instanciaId = UUID.randomUUID();
        UUID pessoaId = UUID.randomUUID();
        ItemInstance instance = instance(instanciaId, ItemInstanceStatus.DISPONIVEL, true, location(UUID.randomUUID(), "Biblioteca"));
        Person person = person(pessoaId, "Maria Silva", true);
        LocalDate previsao = LocalDate.now().plusDays(7);

        when(itemInstanceRepository.findById(instanciaId)).thenReturn(Optional.of(instance));
        when(personRepository.findById(pessoaId)).thenReturn(Optional.of(person));
        when(repository.existsByItemInstanceIdAndReturnDateIsNull(instanciaId)).thenReturn(false);
        when(itemInstanceRepository.save(any(ItemInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(ItemLoan.class))).thenAnswer(invocation -> {
            ItemLoan loan = invocation.getArgument(0);
            loan.setId(UUID.randomUUID());
            return loan;
        });

        var response = service.registerLoan(new ItemLoanCreateDTO(
                instanciaId,
                pessoaId,
                previsao,
                "  Checked out for class use  "
        ));

        ArgumentCaptor<ItemInstance> instanciaCaptor = ArgumentCaptor.forClass(ItemInstance.class);
        ArgumentCaptor<ItemLoan> emprestimoCaptor = ArgumentCaptor.forClass(ItemLoan.class);
        verify(itemInstanceRepository).save(instanciaCaptor.capture());
        verify(repository).save(emprestimoCaptor.capture());

        ItemInstance instanciaAtualizada = instanciaCaptor.getValue();
        assertThat(instanciaAtualizada.getOperationalStatus()).isEqualTo(ItemInstanceStatus.EMPRESTADO);
        assertThat(instanciaAtualizada.getCurrentLocation()).isNull();

        ItemLoan loan = emprestimoCaptor.getValue();
        assertThat(loan.getItemInstance()).isEqualTo(instance);
        assertThat(loan.getPerson()).isEqualTo(person);
        assertThat(loan.getExpectedReturnDate()).isEqualTo(previsao);
        assertThat(loan.getNotes()).isEqualTo("Checked out for class use");
        assertThat(response.pessoaId()).isEqualTo(pessoaId);
        assertThat(response.itemInstanceId()).isEqualTo(instanciaId);
    }

    @Test
    void shouldPreventLoanOfInstanceUnavailable() {
        UUID instanciaId = UUID.randomUUID();
        UUID pessoaId = UUID.randomUUID();
        ItemInstance instance = instance(instanciaId, ItemInstanceStatus.EM_MOVIMENTACAO, true, location(UUID.randomUUID(), "Biblioteca"));

        when(itemInstanceRepository.findById(instanciaId)).thenReturn(Optional.of(instance));
        when(personRepository.findById(pessoaId)).thenReturn(Optional.of(person(pessoaId, "Maria Silva", true)));

        assertThatThrownBy(() -> service.registerLoan(new ItemLoanCreateDTO(instanciaId, pessoaId, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("available instances can be loaned");

        verify(itemInstanceRepository, never()).save(any(ItemInstance.class));
        verify(repository, never()).save(any(ItemLoan.class));
    }

    @Test
    void shouldPreventLoanOpenDuplicate() {
        UUID instanciaId = UUID.randomUUID();
        UUID pessoaId = UUID.randomUUID();
        ItemInstance instance = instance(instanciaId, ItemInstanceStatus.DISPONIVEL, true, location(UUID.randomUUID(), "Biblioteca"));

        when(itemInstanceRepository.findById(instanciaId)).thenReturn(Optional.of(instance));
        when(personRepository.findById(pessoaId)).thenReturn(Optional.of(person(pessoaId, "Maria Silva", true)));
        when(repository.existsByItemInstanceIdAndReturnDateIsNull(instanciaId)).thenReturn(true);

        assertThatThrownBy(() -> service.registerLoan(new ItemLoanCreateDTO(instanciaId, pessoaId, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("open loan for this instance");

        verify(itemInstanceRepository, never()).save(any(ItemInstance.class));
        verify(repository, never()).save(any(ItemLoan.class));
    }

    @Test
    void shouldRegisterReturnEndingLoanAndUpdatingInstance() {
        UUID instanciaId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        ItemInstance instance = instance(instanciaId, ItemInstanceStatus.EMPRESTADO, true, null);
        Person person = person(UUID.randomUUID(), "Maria Silva", true);
        StorageLocation returnLocation = location(locationId, "Biblioteca");
        ItemLoan loan = loan(instance, person);

        when(repository.findByItemInstanceIdAndReturnDateIsNull(instanciaId)).thenReturn(Optional.of(loan));
        when(storageLocationRepository.findById(locationId)).thenReturn(Optional.of(returnLocation));
        when(itemInstanceRepository.save(any(ItemInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(ItemLoan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.registerReturn(new ItemLoanReturnDTO(
                instanciaId,
                locationId,
                "  Devolvido sem avarias  "
        ));

        ArgumentCaptor<ItemInstance> instanciaCaptor = ArgumentCaptor.forClass(ItemInstance.class);
        ArgumentCaptor<ItemLoan> emprestimoCaptor = ArgumentCaptor.forClass(ItemLoan.class);
        verify(itemInstanceRepository).save(instanciaCaptor.capture());
        verify(repository).save(emprestimoCaptor.capture());

        ItemInstance instanciaAtualizada = instanciaCaptor.getValue();
        assertThat(instanciaAtualizada.getOperationalStatus()).isEqualTo(ItemInstanceStatus.DISPONIVEL);
        assertThat(instanciaAtualizada.getCurrentLocation()).isEqualTo(returnLocation);

        ItemLoan emprestimoAtualizado = emprestimoCaptor.getValue();
        assertThat(emprestimoAtualizado.getReturnDate()).isNotNull();
        assertThat(emprestimoAtualizado.getNotes()).isEqualTo("Devolvido sem avarias");
        assertThat(response.returnDate()).isNotNull();
        assertThat(response.itemInstanceId()).isEqualTo(instanciaId);
    }

    @Test
    void shouldPreventReturnWithoutLoanOpen() {
        UUID instanciaId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        when(repository.findByItemInstanceIdAndReturnDateIsNull(instanciaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerReturn(new ItemLoanReturnDTO(instanciaId, locationId, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("open loan for this instance");

        verify(itemInstanceRepository, never()).save(any(ItemInstance.class));
        verify(repository, never()).save(any(ItemLoan.class));
    }

    @Test
    void shouldPreventReturnWhenInstanceNotIsLoaned() {
        UUID instanciaId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        ItemInstance instance = instance(instanciaId, ItemInstanceStatus.DISPONIVEL, true, null);
        ItemLoan loan = loan(instance, person(UUID.randomUUID(), "Maria Silva", true));

        when(repository.findByItemInstanceIdAndReturnDateIsNull(instanciaId)).thenReturn(Optional.of(loan));
        when(storageLocationRepository.findById(locationId)).thenReturn(Optional.of(location(locationId, "Biblioteca")));

        assertThatThrownBy(() -> service.registerReturn(new ItemLoanReturnDTO(instanciaId, locationId, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be loaned");

        verify(itemInstanceRepository, never()).save(any(ItemInstance.class));
        verify(repository, never()).save(any(ItemLoan.class));
    }

    private ItemInstance instance(UUID id, ItemInstanceStatus status, boolean active, StorageLocation location) {
        ItemInstance instance = new ItemInstance();
        instance.setId(id);
        instance.setMainItem(new MainItem());
        instance.setIdentifier("LIV-001");
        instance.setOperationalStatus(status);
        instance.setActive(active);
        instance.setCurrentLocation(location);
        return instance;
    }

    private Person person(UUID id, String name, boolean active) {
        Person person = new Person();
        person.setId(id);
        person.setName(name);
        person.setActive(active);
        return person;
    }

    private StorageLocation location(UUID id, String name) {
        StorageLocation location = new StorageLocation();
        location.setId(id);
        location.setName(name);
        location.setActive(true);
        return location;
    }

    private ItemLoan loan(ItemInstance instance, Person person) {
        ItemLoan loan = new ItemLoan();
        loan.setId(UUID.randomUUID());
        loan.setItemInstance(instance);
        loan.setPerson(person);
        loan.setNotes("Loan inicial");
        return loan;
    }
}

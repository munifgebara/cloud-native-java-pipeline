package br.com.stella.api.service;

import br.com.stella.api.dto.DashboardLocationQuantityDTO;
import br.com.stella.api.dto.DashboardCategoryQuantityDTO;
import br.com.stella.api.entity.ItemInstanceStatus;
import br.com.stella.api.repository.ItemInstanceRepository;
import br.com.stella.api.repository.MainItemRepository;
import br.com.stella.api.repository.StorageLocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private PersonService personService;

    @Mock
    private MainItemRepository mainItemRepository;

    @Mock
    private ItemInstanceRepository itemInstanceRepository;

    @Mock
    private StorageLocationRepository storageLocationRepository;

    @Mock
    private VectorSearchMetricsService vectorSearchMetricsService;

    @InjectMocks
    private DashboardService service;

    @Test
    void deveCarregarResumoDoInventario() {
        var locationId = UUID.randomUUID();
        var categoryId = UUID.randomUUID();
        var locais = List.of(new DashboardLocationQuantityDTO(locationId, "Biblioteca", 12));
        var categories = List.of(new DashboardCategoryQuantityDTO(categoryId, "Livros", 7));

        when(personService.contarPessoasAtivas()).thenReturn(4L);
        when(mainItemRepository.countByActiveTrue()).thenReturn(10L);
        when(mainItemRepository.countByActiveTrueAndImageObjectKeyIsNull()).thenReturn(2L);
        when(mainItemRepository.countItemsRegisteredByAi()).thenReturn(3L);
        when(itemInstanceRepository.countByActiveTrue()).thenReturn(25L);
        when(itemInstanceRepository.countByActiveTrueAndOperationalStatus(ItemInstanceStatus.DISPONIVEL)).thenReturn(18L);
        when(itemInstanceRepository.countByActiveTrueAndOperationalStatus(ItemInstanceStatus.EMPRESTADO)).thenReturn(5L);
        when(storageLocationRepository.countByActiveTrue()).thenReturn(3L);
        when(vectorSearchMetricsService.countQueries()).thenReturn(11L);
        when(itemInstanceRepository.findLocationsWithMostItems(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(locais);
        when(mainItemRepository.findCategoriesWithMostItems(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(categories);

        var resumo = service.carregarResumo();

        assertThat(resumo.quantidadePessoas()).isEqualTo(4);
        assertThat(resumo.quantidadeItensMestre()).isEqualTo(10);
        assertThat(resumo.quantidadeInstancias()).isEqualTo(25);
        assertThat(resumo.quantidadeInstanciasDisponiveis()).isEqualTo(18);
        assertThat(resumo.quantidadeInstanciasEmprestadas()).isEqualTo(5);
        assertThat(resumo.quantidadeLocais()).isEqualTo(3);
        assertThat(resumo.quantidadeItensSemImagem()).isEqualTo(2);
        assertThat(resumo.quantidadeItensCadastradosPorIa()).isEqualTo(3);
        assertThat(resumo.vectorQueryCount()).isEqualTo(11);
        assertThat(resumo.locaisComMaisItens()).containsExactlyElementsOf(locais);
        assertThat(resumo.categoriasComMaisItens()).containsExactlyElementsOf(categories);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(itemInstanceRepository).findLocationsWithMostItems(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
        verify(mainItemRepository).findCategoriesWithMostItems(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }
}

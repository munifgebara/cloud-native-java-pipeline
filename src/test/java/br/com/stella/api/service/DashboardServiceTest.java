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
    private PersonService pessoaService;

    @Mock
    private MainItemRepository itemMestreRepository;

    @Mock
    private ItemInstanceRepository instanciaItemRepository;

    @Mock
    private StorageLocationRepository localArmazenamentoRepository;

    @Mock
    private VectorSearchMetricsService consultaVetorialMetricasService;

    @InjectMocks
    private DashboardService service;

    @Test
    void deveCarregarResumoDoInventario() {
        var localId = UUID.randomUUID();
        var categoriaId = UUID.randomUUID();
        var locais = List.of(new DashboardLocationQuantityDTO(localId, "Biblioteca", 12));
        var categorias = List.of(new DashboardCategoryQuantityDTO(categoriaId, "Livros", 7));

        when(pessoaService.contarPessoasAtivas()).thenReturn(4L);
        when(itemMestreRepository.countByActiveTrue()).thenReturn(10L);
        when(itemMestreRepository.countByActiveTrueAndImageObjectKeyIsNull()).thenReturn(2L);
        when(itemMestreRepository.contarItensCadastradosPorIa()).thenReturn(3L);
        when(instanciaItemRepository.countByActiveTrue()).thenReturn(25L);
        when(instanciaItemRepository.countByActiveTrueAndOperationalStatus(ItemInstanceStatus.DISPONIVEL)).thenReturn(18L);
        when(instanciaItemRepository.countByActiveTrueAndOperationalStatus(ItemInstanceStatus.EMPRESTADO)).thenReturn(5L);
        when(localArmazenamentoRepository.countByActiveTrue()).thenReturn(3L);
        when(consultaVetorialMetricasService.contarConsultas()).thenReturn(11L);
        when(instanciaItemRepository.buscarLocaisComMaisItens(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(locais);
        when(itemMestreRepository.buscarCategoriasComMaisItens(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(categorias);

        var resumo = service.carregarResumo();

        assertThat(resumo.quantidadePessoas()).isEqualTo(4);
        assertThat(resumo.quantidadeItensMestre()).isEqualTo(10);
        assertThat(resumo.quantidadeInstancias()).isEqualTo(25);
        assertThat(resumo.quantidadeInstanciasDisponiveis()).isEqualTo(18);
        assertThat(resumo.quantidadeInstanciasEmprestadas()).isEqualTo(5);
        assertThat(resumo.quantidadeLocais()).isEqualTo(3);
        assertThat(resumo.quantidadeItensSemImagem()).isEqualTo(2);
        assertThat(resumo.quantidadeItensCadastradosPorIa()).isEqualTo(3);
        assertThat(resumo.quantidadeConsultasVetoriais()).isEqualTo(11);
        assertThat(resumo.locaisComMaisItens()).containsExactlyElementsOf(locais);
        assertThat(resumo.categoriasComMaisItens()).containsExactlyElementsOf(categorias);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(instanciaItemRepository).buscarLocaisComMaisItens(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
        verify(itemMestreRepository).buscarCategoriasComMaisItens(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }
}

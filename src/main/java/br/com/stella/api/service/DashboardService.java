package br.com.stella.api.service;

import br.com.stella.api.dto.DashboardSummaryDTO;
import br.com.stella.api.entity.ItemInstanceStatus;
import br.com.stella.api.repository.ItemInstanceRepository;
import br.com.stella.api.repository.MainItemRepository;
import br.com.stella.api.repository.StorageLocationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for consolidating the inventory operational indicators
 * for display on the application dashboard.
 *
 * <p>Aggregates counts of persons, main items, instances by status,
 * locations, items without images, AI-registered items, and vector queries,
 * as well as rankings of locations and categories with the most items.</p>
 */
@Service
public class DashboardService {

    private static final int LIMITE_LOCAIS_COM_MAIS_ITENS = 5;
    private static final int LIMITE_CATEGORIAS_COM_MAIS_ITENS = 5;

    private final PersonService pessoaService;
    private final MainItemRepository itemMestreRepository;
    private final ItemInstanceRepository instanciaItemRepository;
    private final StorageLocationRepository localArmazenamentoRepository;
    private final ConsultaVetorialMetricasService consultaVetorialMetricasService;

    public DashboardService(
            PersonService pessoaService,
            MainItemRepository itemMestreRepository,
            ItemInstanceRepository instanciaItemRepository,
            StorageLocationRepository localArmazenamentoRepository,
            ConsultaVetorialMetricasService consultaVetorialMetricasService
    ) {
        this.pessoaService = pessoaService;
        this.itemMestreRepository = itemMestreRepository;
        this.instanciaItemRepository = instanciaItemRepository;
        this.localArmazenamentoRepository = localArmazenamentoRepository;
        this.consultaVetorialMetricasService = consultaVetorialMetricasService;
    }

    /**
     * Loads and returns the consolidated inventory summary.
     *
     * @return DTO with all dashboard indicators and rankings
     */
    @Transactional(readOnly = true)
    public DashboardSummaryDTO carregarResumo() {
        return new DashboardSummaryDTO(
                pessoaService.contarPessoasAtivas(),
                itemMestreRepository.countByActiveTrue(),
                instanciaItemRepository.countByActiveTrue(),
                instanciaItemRepository.countByActiveTrueAndOperationalStatus(ItemInstanceStatus.DISPONIVEL),
                instanciaItemRepository.countByActiveTrueAndOperationalStatus(ItemInstanceStatus.EMPRESTADO),
                localArmazenamentoRepository.countByActiveTrue(),
                itemMestreRepository.countByActiveTrueAndImagemObjectKeyIsNull(),
                itemMestreRepository.contarItensCadastradosPorIa(),
                consultaVetorialMetricasService.contarConsultas(),
                instanciaItemRepository.buscarLocaisComMaisItens(PageRequest.of(0, LIMITE_LOCAIS_COM_MAIS_ITENS)),
                itemMestreRepository.buscarCategoriasComMaisItens(PageRequest.of(0, LIMITE_CATEGORIAS_COM_MAIS_ITENS))
        );
    }
}

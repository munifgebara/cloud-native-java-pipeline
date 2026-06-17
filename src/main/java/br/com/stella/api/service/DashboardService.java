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

    private final PersonService personService;
    private final MainItemRepository mainItemRepository;
    private final ItemInstanceRepository itemInstanceRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final VectorSearchMetricsService vectorSearchMetricsService;

    public DashboardService(
            PersonService personService,
            MainItemRepository mainItemRepository,
            ItemInstanceRepository itemInstanceRepository,
            StorageLocationRepository storageLocationRepository,
            VectorSearchMetricsService vectorSearchMetricsService
    ) {
        this.personService = personService;
        this.mainItemRepository = mainItemRepository;
        this.itemInstanceRepository = itemInstanceRepository;
        this.storageLocationRepository = storageLocationRepository;
        this.vectorSearchMetricsService = vectorSearchMetricsService;
    }

    /**
     * Loads and returns the consolidated inventory summary.
     *
     * @return DTO with all dashboard indicators and rankings
     */
    @Transactional(readOnly = true)
    public DashboardSummaryDTO carregarResumo() {
        return new DashboardSummaryDTO(
                personService.countActivePeople(),
                mainItemRepository.countByActiveTrue(),
                itemInstanceRepository.countByActiveTrue(),
                itemInstanceRepository.countByActiveTrueAndOperationalStatus(ItemInstanceStatus.DISPONIVEL),
                itemInstanceRepository.countByActiveTrueAndOperationalStatus(ItemInstanceStatus.EMPRESTADO),
                storageLocationRepository.countByActiveTrue(),
                mainItemRepository.countByActiveTrueAndImageObjectKeyIsNull(),
                mainItemRepository.countItemsRegisteredByAi(),
                vectorSearchMetricsService.countQueries(),
                itemInstanceRepository.findLocationsWithMostItems(PageRequest.of(0, LIMITE_LOCAIS_COM_MAIS_ITENS)),
                mainItemRepository.findCategoriesWithMostItems(PageRequest.of(0, LIMITE_CATEGORIAS_COM_MAIS_ITENS))
        );
    }
}

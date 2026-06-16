package br.com.stella.api.controller;

import br.com.stella.api.dto.DashboardSummaryDTO;
import br.com.stella.api.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the inventory operational summary dashboard.
 *
 * <p>Provides consolidated indicators — total items, instances, persons and
 * distribution by category and location — for the application's home screen.</p>
 */
@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Constructs the controller injecting the dashboard service.
     *
     * @param dashboardService service responsible for calculating the summary indicators
     */
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Returns the consolidated inventory summary for display on the dashboard.
     *
     * @return DTO with totals and distributions of items, instances, persons,
     *         categories and storage locations
     */
    @GetMapping("/api/v0/dashboard/resumo")
    public DashboardSummaryDTO resumo() {
        return dashboardService.carregarResumo();
    }
}

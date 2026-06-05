package br.com.munif.stella.api.controller;

import br.com.munif.stella.api.dto.DashboardResumoDTO;
import br.com.munif.stella.api.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/v0/dashboard/resumo")
    public DashboardResumoDTO resumo() {
        return dashboardService.carregarResumo();
    }
}

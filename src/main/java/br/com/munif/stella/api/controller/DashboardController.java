package br.com.munif.stella.api.controller;

import br.com.munif.stella.api.dto.DashboardResumoDTO;
import br.com.munif.stella.api.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST para o dashboard de resumo operacional do inventário.
 *
 * <p>Fornece indicadores consolidados — total de itens, instâncias, pessoas e
 * distribuição por categoria e local — para a tela inicial da aplicação.</p>
 */
@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Constrói o controller injetando o serviço de dashboard.
     *
     * @param dashboardService serviço responsável por calcular os indicadores do resumo
     */
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Retorna o resumo consolidado do inventário para exibição no dashboard.
     *
     * @return DTO com totais e distribuições de itens, instâncias, pessoas,
     *         categorias e locais de armazenamento
     */
    @GetMapping("/api/v0/dashboard/resumo")
    public DashboardResumoDTO resumo() {
        return dashboardService.carregarResumo();
    }
}

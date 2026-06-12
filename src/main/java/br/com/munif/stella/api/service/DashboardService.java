package br.com.munif.stella.api.service;

import br.com.munif.stella.api.dto.DashboardResumoDTO;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import br.com.munif.stella.api.repository.InstanciaItemRepository;
import br.com.munif.stella.api.repository.ItemMestreRepository;
import br.com.munif.stella.api.repository.LocalArmazenamentoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final int LIMITE_LOCAIS_COM_MAIS_ITENS = 5;
    private static final int LIMITE_CATEGORIAS_COM_MAIS_ITENS = 5;

    private final PessoaService pessoaService;
    private final ItemMestreRepository itemMestreRepository;
    private final InstanciaItemRepository instanciaItemRepository;
    private final LocalArmazenamentoRepository localArmazenamentoRepository;
    private final ConsultaVetorialMetricasService consultaVetorialMetricasService;

    public DashboardService(
            PessoaService pessoaService,
            ItemMestreRepository itemMestreRepository,
            InstanciaItemRepository instanciaItemRepository,
            LocalArmazenamentoRepository localArmazenamentoRepository,
            ConsultaVetorialMetricasService consultaVetorialMetricasService
    ) {
        this.pessoaService = pessoaService;
        this.itemMestreRepository = itemMestreRepository;
        this.instanciaItemRepository = instanciaItemRepository;
        this.localArmazenamentoRepository = localArmazenamentoRepository;
        this.consultaVetorialMetricasService = consultaVetorialMetricasService;
    }

    @Transactional(readOnly = true)
    public DashboardResumoDTO carregarResumo() {
        return new DashboardResumoDTO(
                pessoaService.contarPessoasAtivas(),
                itemMestreRepository.countByAtivoTrue(),
                instanciaItemRepository.countByAtivoTrue(),
                instanciaItemRepository.countByAtivoTrueAndStatusOperacional(StatusOperacionalInstancia.DISPONIVEL),
                instanciaItemRepository.countByAtivoTrueAndStatusOperacional(StatusOperacionalInstancia.EMPRESTADO),
                localArmazenamentoRepository.countByAtivoTrue(),
                itemMestreRepository.countByAtivoTrueAndImagemObjectKeyIsNull(),
                itemMestreRepository.contarItensCadastradosPorIa(),
                consultaVetorialMetricasService.contarConsultas(),
                instanciaItemRepository.buscarLocaisComMaisItens(PageRequest.of(0, LIMITE_LOCAIS_COM_MAIS_ITENS)),
                itemMestreRepository.buscarCategoriasComMaisItens(PageRequest.of(0, LIMITE_CATEGORIAS_COM_MAIS_ITENS))
        );
    }
}

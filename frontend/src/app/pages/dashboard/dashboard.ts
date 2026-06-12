import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { DashboardResumo, DashboardService } from '../../core/dashboard';
import { I18nService, TranslatePipe } from '../../core/i18n/i18n';
import { categoriaIconClass } from '../../core/categoria/categoria';
import { ConsultaSemanticaItem, ItemMestreService } from '../../core/item-mestre/item-mestre';

@Component({
  selector: 'app-dashboard',
  imports: [FormsModule, ButtonModule, CardModule, InputTextModule, RouterLink, TranslatePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly itemMestreService = inject(ItemMestreService);
  private readonly i18n = inject(I18nService);

  resumo = signal<DashboardResumo | null>(null);
  resultadosSemanticos = signal<ConsultaSemanticaItem[]>([]);
  loading = signal(true);
  loadingSemantica = signal(false);
  errorMessage = signal('');
  semanticErrorMessage = signal('');
  consultaSemantica = '';

  ngOnInit(): void {
    this.dashboardService.carregarResumo().subscribe({
      next: (resumo) => {
        this.resumo.set(resumo);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('dashboard.loadError'));
        this.loading.set(false);
      },
    });
  }

  buscarSemanticamente(): void {
    const consulta = this.consultaSemantica.trim();
    this.semanticErrorMessage.set('');

    if (!consulta) {
      this.resultadosSemanticos.set([]);
      return;
    }

    this.loadingSemantica.set(true);
    this.itemMestreService.buscarSemanticamente(consulta).subscribe({
      next: (resultados) => {
        this.resultadosSemanticos.set(resultados);
        this.loadingSemantica.set(false);
        this.carregarResumo();
      },
      error: () => {
        this.semanticErrorMessage.set(this.i18n.translate('dashboard.semanticSearchError'));
        this.loadingSemantica.set(false);
      },
    });
  }

  semanticIconClass(item: ConsultaSemanticaItem): string {
    return categoriaIconClass(item.categoriaIcone);
  }

  similaridadePercentual(item: ConsultaSemanticaItem): string {
    return `${Math.round(item.similaridade * 100)}%`;
  }

  instanciasResumo(item: ConsultaSemanticaItem): string {
    if (!item.instancias.length) {
      return this.i18n.translate('dashboard.semanticNoInstances');
    }

    return item.instancias
      .slice(0, 3)
      .map((instancia) => instancia.identificador || instancia.patrimonio || instancia.numeroSerie || this.i18n.translate('dashboard.semanticUnnamedInstance'))
      .join(', ');
  }

  locaisResumo(item: ConsultaSemanticaItem): string {
    if (!item.locaisProvaveis.length) {
      return this.i18n.translate('dashboard.semanticNoLocations');
    }

    return item.locaisProvaveis
      .slice(0, 3)
      .map((local) => `${local.nome} (${local.quantidade})`)
      .join(', ');
  }

  private carregarResumo(): void {
    this.dashboardService.carregarResumo().subscribe({
      next: (resumo) => this.resumo.set(resumo),
    });
  }
}

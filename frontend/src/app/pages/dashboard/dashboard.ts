import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DashboardSummary, DashboardService } from '../../core/dashboard';
import { I18nService, TranslatePipe } from '../../core/i18n/i18n';
import { categoriaIconClass } from '../../core/categoria/categoria';
import { SemanticSearchItem, MainItemService } from '../../core/item-mestre/item-mestre';
import { StellaAiPanelComponent } from '../../shared/design-system/stella-ai-panel/stella-ai-panel';
import { StellaEmptyStateComponent } from '../../shared/design-system/stella-empty-state/stella-empty-state';
import { StellaMetricCardComponent } from '../../shared/design-system/stella-metric-card/stella-metric-card';
import { StellaPageHeaderComponent } from '../../shared/design-system/stella-page-header/stella-page-header';
import { StellaStateMessageComponent } from '../../shared/design-system/stella-state-message/stella-state-message';

@Component({
  selector: 'app-dashboard',
  imports: [
    FormsModule,
    ButtonModule,
    InputTextModule,
    RouterLink,
    TranslatePipe,
    StellaAiPanelComponent,
    StellaEmptyStateComponent,
    StellaMetricCardComponent,
    StellaPageHeaderComponent,
    StellaStateMessageComponent,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly itemMestreService = inject(MainItemService);
  private readonly i18n = inject(I18nService);

  resumo = signal<DashboardSummary | null>(null);
  resultadosSemanticos = signal<SemanticSearchItem[]>([]);
  loading = signal(true);
  loadingSemantica = signal(false);
  errorMessage = signal('');
  semanticErrorMessage = signal('');
  consultaSemantica = '';

  ngOnInit(): void {
    this.dashboardService.carregarSummary().subscribe({
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
        this.carregarSummary();
      },
      error: () => {
        this.semanticErrorMessage.set(this.i18n.translate('dashboard.semanticSearchError'));
        this.loadingSemantica.set(false);
      },
    });
  }

  semanticIconClass(item: SemanticSearchItem): string {
    return categoriaIconClass(item.categoryIcon);
  }

  similaridadePercentual(item: SemanticSearchItem): string {
    return `${Math.round(item.similaridade * 100)}%`;
  }

  instanciasSummary(item: SemanticSearchItem): string {
    if (!item.instancias.length) {
      return this.i18n.translate('dashboard.semanticNoInstances');
    }

    return item.instancias
      .slice(0, 3)
      .map((instancia) => instancia.identifier || instancia.assetTag || instancia.serialNumber || this.i18n.translate('dashboard.semanticUnnamedInstance'))
      .join(', ');
  }

  locaisSummary(item: SemanticSearchItem): string {
    if (!item.probableLocations.length) {
      return this.i18n.translate('dashboard.semanticNoLocations');
    }

    return item.probableLocations
      .slice(0, 3)
      .map((local) => `${local.name} (${local.quantidade})`)
      .join(', ');
  }

  private carregarSummary(): void {
    this.dashboardService.carregarSummary().subscribe({
      next: (resumo) => this.resumo.set(resumo),
    });
  }
}

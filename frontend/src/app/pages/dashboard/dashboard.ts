import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';
import { DashboardResumo, DashboardService } from '../../core/dashboard';
import { I18nService, TranslatePipe } from '../../core/i18n/i18n';

@Component({
  selector: 'app-dashboard',
  imports: [CardModule, RouterLink, TranslatePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly i18n = inject(I18nService);

  resumo = signal<DashboardResumo | null>(null);
  loading = signal(true);
  errorMessage = signal('');

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
}

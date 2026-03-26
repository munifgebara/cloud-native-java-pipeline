import { Component, OnInit, inject, signal } from '@angular/core';
import { CardModule } from 'primeng/card';
import { DashboardResumo, DashboardService } from '../../core/dashboard';

@Component({
  selector: 'app-dashboard',
  imports: [CardModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);

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
        this.errorMessage.set('Não foi possível carregar o dashboard.');
        this.loading.set(false);
      },
    });
  }
}

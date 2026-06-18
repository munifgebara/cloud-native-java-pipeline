import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface DashboardSummary {
  peopleCount: number;
  mainItemCount: number;
  instanceCount: number;
  availableInstanceCount: number;
  loanedInstanceCount: number;
  locationCount: number;
  itemsWithoutImageCount: number;
  quantidadeItensCadastradosPorIa: number;
  quantidadeConsultasVetoriais: number;
  locaisComMaisItens: DashboardLocalQuantidade[];
  categoriasComMaisItens: DashboardCategoryQuantidade[];
}

export interface DashboardLocalQuantidade {
  id: string;
  name: string;
  instanceCount: number;
}

export interface DashboardCategoryQuantidade {
  id: string;
  name: string;
  quantidadeItens: number;
}

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/dashboard`;

  carregarSummary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(`${this.baseUrl}/summary`);
  }
}

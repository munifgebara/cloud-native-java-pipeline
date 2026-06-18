import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface DashboardResumo {
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
  categoriasComMaisItens: DashboardCategoriaQuantidade[];
}

export interface DashboardLocalQuantidade {
  id: string;
  name: string;
  instanceCount: number;
}

export interface DashboardCategoriaQuantidade {
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

  carregarResumo(): Observable<DashboardResumo> {
    return this.http.get<DashboardResumo>(`${this.baseUrl}/summary`);
  }
}

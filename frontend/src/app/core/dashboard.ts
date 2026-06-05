import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface DashboardResumo {
  quantidadePessoas: number;
  quantidadeItensMestre: number;
  quantidadeInstancias: number;
  quantidadeInstanciasDisponiveis: number;
  quantidadeInstanciasEmprestadas: number;
  quantidadeLocais: number;
  locaisComMaisItens: DashboardLocalQuantidade[];
}

export interface DashboardLocalQuantidade {
  id: string;
  nome: string;
  quantidadeInstancias: number;
}

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/dashboard`;

  carregarResumo(): Observable<DashboardResumo> {
    return this.http.get<DashboardResumo>(`${this.baseUrl}/resumo`);
  }
}

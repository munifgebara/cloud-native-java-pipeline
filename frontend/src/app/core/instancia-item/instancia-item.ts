import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type StatusOperacionalInstancia = 'DISPONIVEL' | 'EM_MOVIMENTACAO' | 'EMPRESTADO' | 'INATIVO';

export interface InstanciaItemResumo {
  id: string;
  itemMestreId: string;
  itemMestreNome: string;
  categoriaNome: string | null;
  categoriaIcone: string | null;
  identificador: string | null;
  patrimonio: string | null;
  numeroSerie: string | null;
  statusOperacional: StatusOperacionalInstancia;
  ativa: boolean;
}

export interface InstanciaItemResponse {
  id: string;
  itemMestreId: string;
  itemMestreNome: string;
  categoriaId: string | null;
  categoriaNome: string | null;
  categoriaIcone: string | null;
  identificador: string | null;
  patrimonio: string | null;
  numeroSerie: string | null;
  statusOperacional: StatusOperacionalInstancia;
  observacoes: string | null;
  ativa: boolean;
}

export interface InstanciaItemCreateRequest {
  itemMestreId: string;
  identificador?: string | null;
  patrimonio?: string | null;
  numeroSerie?: string | null;
  statusOperacional?: StatusOperacionalInstancia | null;
  observacoes?: string | null;
  ativa?: boolean | null;
}

export interface InstanciaItemUpdateRequest {
  itemMestreId: string;
  identificador?: string | null;
  patrimonio?: string | null;
  numeroSerie?: string | null;
  statusOperacional?: StatusOperacionalInstancia | null;
  observacoes?: string | null;
  ativa?: boolean | null;
}

@Injectable({
  providedIn: 'root',
})
export class InstanciaItemService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/instancias-item`;

  listar(): Observable<InstanciaItemResumo[]> {
    return this.http.get<InstanciaItemResumo[]>(this.baseUrl);
  }

  buscarPorIdentificador(identificador: string): Observable<InstanciaItemResumo[]> {
    return this.http.get<InstanciaItemResumo[]>(`${this.baseUrl}/buscar`, {
      params: { identificador },
    });
  }

  buscarPorId(id: string): Observable<InstanciaItemResponse> {
    return this.http.get<InstanciaItemResponse>(`${this.baseUrl}/${id}`);
  }

  criar(payload: InstanciaItemCreateRequest): Observable<InstanciaItemResponse> {
    return this.http.post<InstanciaItemResponse>(this.baseUrl, payload);
  }

  atualizar(id: string, payload: InstanciaItemUpdateRequest): Observable<InstanciaItemResponse> {
    return this.http.put<InstanciaItemResponse>(`${this.baseUrl}/${id}`, payload);
  }

  excluir(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ItemMestreResumo {
  id: string;
  nome: string;
  descricao: string | null;
  categoriaId: string | null;
  categoriaNome: string | null;
  categoriaIcone: string | null;
  ativa: boolean;
}

export interface ItemMestreResponse {
  id: string;
  nome: string;
  descricao: string | null;
  observacoes: string | null;
  categoriaId: string | null;
  categoriaNome: string | null;
  categoriaIcone: string | null;
  ativa: boolean;
}

export interface ItemMestreCreateRequest {
  nome: string;
  descricao?: string | null;
  observacoes?: string | null;
  categoriaId?: string | null;
  ativa?: boolean | null;
}

export interface ItemMestreUpdateRequest {
  nome: string;
  descricao?: string | null;
  observacoes?: string | null;
  categoriaId?: string | null;
  ativa?: boolean | null;
}

@Injectable({
  providedIn: 'root',
})
export class ItemMestreService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/itens-mestre`;

  listar(): Observable<ItemMestreResumo[]> {
    return this.http.get<ItemMestreResumo[]>(this.baseUrl);
  }

  buscarPorNome(nome: string): Observable<ItemMestreResumo[]> {
    return this.http.get<ItemMestreResumo[]>(`${this.baseUrl}/buscar`, {
      params: { nome },
    });
  }

  buscarPorId(id: string): Observable<ItemMestreResponse> {
    return this.http.get<ItemMestreResponse>(`${this.baseUrl}/${id}`);
  }

  criar(payload: ItemMestreCreateRequest): Observable<ItemMestreResponse> {
    return this.http.post<ItemMestreResponse>(this.baseUrl, payload);
  }

  atualizar(id: string, payload: ItemMestreUpdateRequest): Observable<ItemMestreResponse> {
    return this.http.put<ItemMestreResponse>(`${this.baseUrl}/${id}`, payload);
  }

  excluir(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

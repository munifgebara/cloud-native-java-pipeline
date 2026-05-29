import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CategoriaResumo {
  id: string;
  nome: string;
  descricao: string | null;
  ativa: boolean;
}

export interface CategoriaResponse {
  id: string;
  nome: string;
  descricao: string | null;
  ativa: boolean;
}

export interface CategoriaCreateRequest {
  nome: string;
  descricao?: string | null;
  ativa?: boolean | null;
}

export interface CategoriaUpdateRequest {
  nome: string;
  descricao?: string | null;
  ativa?: boolean | null;
}

@Injectable({
  providedIn: 'root',
})
export class CategoriaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/categorias`;

  listar(): Observable<CategoriaResumo[]> {
    return this.http.get<CategoriaResumo[]>(this.baseUrl);
  }

  buscarPorNome(nome: string): Observable<CategoriaResumo[]> {
    return this.http.get<CategoriaResumo[]>(`${this.baseUrl}/buscar`, {
      params: { nome },
    });
  }

  buscarPorId(id: string): Observable<CategoriaResponse> {
    return this.http.get<CategoriaResponse>(`${this.baseUrl}/${id}`);
  }

  criar(payload: CategoriaCreateRequest): Observable<CategoriaResponse> {
    return this.http.post<CategoriaResponse>(this.baseUrl, payload);
  }

  atualizar(id: string, payload: CategoriaUpdateRequest): Observable<CategoriaResponse> {
    return this.http.put<CategoriaResponse>(`${this.baseUrl}/${id}`, payload);
  }

  excluir(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

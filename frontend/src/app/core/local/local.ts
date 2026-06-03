import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface LocalResumo {
  id: string;
  nome: string;
  descricao: string | null;
  paiId: string | null;
  paiNome: string | null;
  caminho: string;
  nivel: number;
  ativa: boolean;
}

export interface LocalResponse {
  id: string;
  nome: string;
  descricao: string | null;
  paiId: string | null;
  paiNome: string | null;
  caminho: string;
  nivel: number;
  ativa: boolean;
}

export interface LocalCreateRequest {
  nome: string;
  descricao?: string | null;
  paiId?: string | null;
  ativa?: boolean | null;
}

export interface LocalUpdateRequest {
  nome: string;
  descricao?: string | null;
  paiId?: string | null;
  ativa?: boolean | null;
}

@Injectable({
  providedIn: 'root',
})
export class LocalService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/locais`;

  listar(): Observable<LocalResumo[]> {
    return this.http.get<LocalResumo[]>(this.baseUrl);
  }

  buscarPorNome(nome: string): Observable<LocalResumo[]> {
    return this.http.get<LocalResumo[]>(`${this.baseUrl}/buscar`, {
      params: { nome },
    });
  }

  buscarPorId(id: string): Observable<LocalResponse> {
    return this.http.get<LocalResponse>(`${this.baseUrl}/${id}`);
  }

  criar(payload: LocalCreateRequest): Observable<LocalResponse> {
    return this.http.post<LocalResponse>(this.baseUrl, payload);
  }

  atualizar(id: string, payload: LocalUpdateRequest): Observable<LocalResponse> {
    return this.http.put<LocalResponse>(`${this.baseUrl}/${id}`, payload);
  }

  excluir(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

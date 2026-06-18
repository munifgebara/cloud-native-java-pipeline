import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PessoaResumo {
  id: string;
  name: string;
  taxId: string;
  email: string | null;
  telefonePrincipal: string | null;
}

export interface PessoaResponse {
  id: string;
  name: string;
  taxId: string;
  telefonePrincipal: string | null;
  telefoneSecundario: string | null;
  email: string | null;
  cep: string | null;
  endereco: string | null;
  complemento: string | null;
  bairro: string | null;
  cidade: string | null;
  uf: string | null;
  criadoEm: string;
  alteradoEm: string;
}

export interface PessoaRevisao {
  revisao: number;
  timestamp: string;
  type: 'ADD' | 'MOD' | 'DEL';
  pessoa: PessoaResponse;
  changedFields: string[];
}

export interface PessoaCreateRequest {
  name: string;
  taxId: string;
  telefonePrincipal?: string | null;
  telefoneSecundario?: string | null;
  email?: string | null;
  cep?: string | null;
  endereco?: string | null;
  complemento?: string | null;
  bairro?: string | null;
  cidade?: string | null;
  uf?: string | null;
}

export interface PessoaUpdateRequest {
  name: string;
  telefonePrincipal?: string | null;
  telefoneSecundario?: string | null;
  email?: string | null;
  cep?: string | null;
  endereco?: string | null;
  complemento?: string | null;
  bairro?: string | null;
  cidade?: string | null;
  uf?: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class PessoaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/people`;

  listar(): Observable<PessoaResumo[]> {
    return this.http.get<PessoaResumo[]>(this.baseUrl);
  }

  buscarPorNome(name: string): Observable<PessoaResumo[]> {
    return this.http.get<PessoaResumo[]>(`${this.baseUrl}/search`, {
      params: { name },
    });
  }

  buscarPorId(id: string): Observable<PessoaResponse> {
    return this.http.get<PessoaResponse>(`${this.baseUrl}/${id}`);
  }

  criar(payload: PessoaCreateRequest): Observable<PessoaResponse> {
    return this.http.post<PessoaResponse>(this.baseUrl, payload);
  }

  atualizar(id: string, payload: PessoaUpdateRequest): Observable<PessoaResponse> {
    return this.http.put<PessoaResponse>(`${this.baseUrl}/${id}`, payload);
  }

  listarRevisoes(id: string): Observable<PessoaRevisao[]> {
    return this.http.get<PessoaRevisao[]>(`${this.baseUrl}/${id}/revisions`);
  }

  excluir(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

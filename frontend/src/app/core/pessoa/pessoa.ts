import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PersonSummary {
  id: string;
  name: string;
  taxId: string;
  email: string | null;
  telefonePrincipal: string | null;
}

export interface PersonResponse {
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

export interface PersonRevisao {
  revisao: number;
  timestamp: string;
  type: 'ADD' | 'MOD' | 'DEL';
  pessoa: PersonResponse;
  changedFields: string[];
}

export interface PersonCreateRequest {
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

export interface PersonUpdateRequest {
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
export class PersonService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/people`;

  listar(): Observable<PersonSummary[]> {
    return this.http.get<PersonSummary[]>(this.baseUrl);
  }

  buscarPorNome(name: string): Observable<PersonSummary[]> {
    return this.http.get<PersonSummary[]>(`${this.baseUrl}/search`, {
      params: { name },
    });
  }

  buscarPorId(id: string): Observable<PersonResponse> {
    return this.http.get<PersonResponse>(`${this.baseUrl}/${id}`);
  }

  criar(payload: PersonCreateRequest): Observable<PersonResponse> {
    return this.http.post<PersonResponse>(this.baseUrl, payload);
  }

  update(id: string, payload: PersonUpdateRequest): Observable<PersonResponse> {
    return this.http.put<PersonResponse>(`${this.baseUrl}/${id}`, payload);
  }

  listarRevisoes(id: string): Observable<PersonRevisao[]> {
    return this.http.get<PersonRevisao[]>(`${this.baseUrl}/${id}/revisions`);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

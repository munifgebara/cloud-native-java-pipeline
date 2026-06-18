import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PersonSummary {
  id: string;
  name: string;
  taxId: string;
  email: string | null;
  primaryPhone: string | null;
}

export interface PersonResponse {
  id: string;
  name: string;
  taxId: string;
  primaryPhone: string | null;
  secondaryPhone: string | null;
  email: string | null;
  zipCode: string | null;
  address: string | null;
  complement: string | null;
  neighborhood: string | null;
  city: string | null;
  state: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PersonRevision {
  revision: number;
  timestamp: string;
  type: 'ADD' | 'MOD' | 'DEL';
  person: PersonResponse;
  changedFields: string[];
}

export interface PersonCreateRequest {
  name: string;
  taxId: string;
  primaryPhone?: string | null;
  secondaryPhone?: string | null;
  email?: string | null;
  zipCode?: string | null;
  address?: string | null;
  complement?: string | null;
  neighborhood?: string | null;
  city?: string | null;
  state?: string | null;
}

export interface PersonUpdateRequest {
  name: string;
  primaryPhone?: string | null;
  secondaryPhone?: string | null;
  email?: string | null;
  zipCode?: string | null;
  address?: string | null;
  complement?: string | null;
  neighborhood?: string | null;
  city?: string | null;
  state?: string | null;
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

  listRevisions(id: string): Observable<PersonRevision[]> {
    return this.http.get<PersonRevision[]>(`${this.baseUrl}/${id}/revisions`);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

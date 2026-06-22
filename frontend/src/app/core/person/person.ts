import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface PersonSummary {
  id: string;
  name: string;
  photoUrl: string | null;
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
  photoUrl: string | null;
  photoContentType: string | null;
  photoSizeBytes: number | null;
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
    return this.http.get<PersonSummary[]>(this.baseUrl).pipe(
      map((people) => people.map((person) => this.withAbsolutePhotoUrl(person))),
    );
  }

  buscarPorNome(name: string): Observable<PersonSummary[]> {
    return this.http.get<PersonSummary[]>(`${this.baseUrl}/search`, {
      params: { name },
    }).pipe(map((people) => people.map((person) => this.withAbsolutePhotoUrl(person))));
  }

  buscarPorId(id: string): Observable<PersonResponse> {
    return this.http.get<PersonResponse>(`${this.baseUrl}/${id}`).pipe(
      map((person) => this.withAbsolutePhotoUrl(person)),
    );
  }

  criar(payload: PersonCreateRequest): Observable<PersonResponse> {
    return this.http.post<PersonResponse>(this.baseUrl, payload).pipe(
      map((person) => this.withAbsolutePhotoUrl(person)),
    );
  }

  update(id: string, payload: PersonUpdateRequest): Observable<PersonResponse> {
    return this.http.put<PersonResponse>(`${this.baseUrl}/${id}`, payload).pipe(
      map((person) => this.withAbsolutePhotoUrl(person)),
    );
  }

  atualizarFoto(id: string, arquivo: File): Observable<PersonResponse> {
    const formData = new FormData();
    formData.append('file', arquivo);

    return this.http.post<PersonResponse>(`${this.baseUrl}/${id}/photo`, formData).pipe(
      map((person) => this.withAbsolutePhotoUrl(person)),
    );
  }

  removerFoto(id: string): Observable<PersonResponse> {
    return this.http.delete<PersonResponse>(`${this.baseUrl}/${id}/photo`).pipe(
      map((person) => this.withAbsolutePhotoUrl(person)),
    );
  }

  listRevisions(id: string): Observable<PersonRevision[]> {
    return this.http.get<PersonRevision[]>(`${this.baseUrl}/${id}/revisions`);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  private withAbsolutePhotoUrl<T extends { photoUrl: string | null }>(person: T): T {
    if (!person.photoUrl || person.photoUrl.startsWith('http')) {
      return person;
    }

    return {
      ...person,
      photoUrl: `${environment.apiBaseUrl}${person.photoUrl}`,
    };
  }
}

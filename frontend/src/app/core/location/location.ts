import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface LocationSummary {
  id: string;
  name: string;
  description: string | null;
  paiId: string | null;
  paiNome: string | null;
  caminho: string;
  nivel: number;
  imageUrl: string | null;
  active: boolean;
}

export interface LocationResponse {
  id: string;
  name: string;
  description: string | null;
  paiId: string | null;
  paiNome: string | null;
  caminho: string;
  nivel: number;
  imageUrl: string | null;
  imagemContentType: string | null;
  imagemTamanhoBytes: number | null;
  active: boolean;
}

export interface LocationCreateRequest {
  name: string;
  description?: string | null;
  paiId?: string | null;
  active?: boolean | null;
}

export interface LocationUpdateRequest {
  name: string;
  description?: string | null;
  paiId?: string | null;
  active?: boolean | null;
}

@Injectable({
  providedIn: 'root',
})
export class LocationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/locations`;

  listar(): Observable<LocationSummary[]> {
    return this.http.get<LocationSummary[]>(this.baseUrl).pipe(
      map((locations) => locations.map((local) => this.withAbsoluteImageUrl(local)))
    );
  }

  buscarPorNome(name: string): Observable<LocationSummary[]> {
    return this.http.get<LocationSummary[]>(`${this.baseUrl}/search`, {
      params: { name },
    }).pipe(map((locations) => locations.map((local) => this.withAbsoluteImageUrl(local))));
  }

  buscarPorId(id: string): Observable<LocationResponse> {
    return this.http.get<LocationResponse>(`${this.baseUrl}/${id}`).pipe(
      map((local) => this.withAbsoluteImageUrl(local))
    );
  }

  criar(payload: LocationCreateRequest): Observable<LocationResponse> {
    return this.http.post<LocationResponse>(this.baseUrl, payload).pipe(
      map((local) => this.withAbsoluteImageUrl(local))
    );
  }

  update(id: string, payload: LocationUpdateRequest): Observable<LocationResponse> {
    return this.http.put<LocationResponse>(`${this.baseUrl}/${id}`, payload).pipe(
      map((local) => this.withAbsoluteImageUrl(local))
    );
  }

  atualizarImage(id: string, arquivo: File): Observable<LocationResponse> {
    const formData = new FormData();
    formData.append('arquivo', arquivo);

    return this.http.post<LocationResponse>(`${this.baseUrl}/${id}/imagem`, formData).pipe(
      map((local) => this.withAbsoluteImageUrl(local))
    );
  }

  removerImage(id: string): Observable<LocationResponse> {
    return this.http.delete<LocationResponse>(`${this.baseUrl}/${id}/imagem`).pipe(
      map((local) => this.withAbsoluteImageUrl(local))
    );
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  private withAbsoluteImageUrl<T extends { imageUrl: string | null }>(local: T): T {
    if (!local.imageUrl || local.imageUrl.startsWith('http')) {
      return local;
    }

    return {
      ...local,
      imageUrl: `${environment.apiBaseUrl}${local.imageUrl}`,
    };
  }
}

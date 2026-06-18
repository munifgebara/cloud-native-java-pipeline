import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface LocalResumo {
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

export interface LocalResponse {
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

export interface LocalCreateRequest {
  name: string;
  description?: string | null;
  paiId?: string | null;
  active?: boolean | null;
}

export interface LocalUpdateRequest {
  name: string;
  description?: string | null;
  paiId?: string | null;
  active?: boolean | null;
}

@Injectable({
  providedIn: 'root',
})
export class LocalService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/locations`;

  listar(): Observable<LocalResumo[]> {
    return this.http.get<LocalResumo[]>(this.baseUrl).pipe(
      map((locais) => locais.map((local) => this.withAbsoluteImageUrl(local)))
    );
  }

  buscarPorNome(name: string): Observable<LocalResumo[]> {
    return this.http.get<LocalResumo[]>(`${this.baseUrl}/search`, {
      params: { name },
    }).pipe(map((locais) => locais.map((local) => this.withAbsoluteImageUrl(local))));
  }

  buscarPorId(id: string): Observable<LocalResponse> {
    return this.http.get<LocalResponse>(`${this.baseUrl}/${id}`).pipe(
      map((local) => this.withAbsoluteImageUrl(local))
    );
  }

  criar(payload: LocalCreateRequest): Observable<LocalResponse> {
    return this.http.post<LocalResponse>(this.baseUrl, payload).pipe(
      map((local) => this.withAbsoluteImageUrl(local))
    );
  }

  atualizar(id: string, payload: LocalUpdateRequest): Observable<LocalResponse> {
    return this.http.put<LocalResponse>(`${this.baseUrl}/${id}`, payload).pipe(
      map((local) => this.withAbsoluteImageUrl(local))
    );
  }

  atualizarImagem(id: string, arquivo: File): Observable<LocalResponse> {
    const formData = new FormData();
    formData.append('arquivo', arquivo);

    return this.http.post<LocalResponse>(`${this.baseUrl}/${id}/imagem`, formData).pipe(
      map((local) => this.withAbsoluteImageUrl(local))
    );
  }

  removerImagem(id: string): Observable<LocalResponse> {
    return this.http.delete<LocalResponse>(`${this.baseUrl}/${id}/imagem`).pipe(
      map((local) => this.withAbsoluteImageUrl(local))
    );
  }

  excluir(id: string): Observable<void> {
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

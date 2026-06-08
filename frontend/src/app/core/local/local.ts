import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface LocalResumo {
  id: string;
  nome: string;
  descricao: string | null;
  paiId: string | null;
  paiNome: string | null;
  caminho: string;
  nivel: number;
  imagemUrl: string | null;
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
  imagemUrl: string | null;
  imagemContentType: string | null;
  imagemTamanhoBytes: number | null;
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
    return this.http.get<LocalResumo[]>(this.baseUrl).pipe(
      map((locais) => locais.map((local) => this.withAbsoluteImageUrl(local)))
    );
  }

  buscarPorNome(nome: string): Observable<LocalResumo[]> {
    return this.http.get<LocalResumo[]>(`${this.baseUrl}/buscar`, {
      params: { nome },
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

  private withAbsoluteImageUrl<T extends { imagemUrl: string | null }>(local: T): T {
    if (!local.imagemUrl || local.imagemUrl.startsWith('http')) {
      return local;
    }

    return {
      ...local,
      imagemUrl: `${environment.apiBaseUrl}${local.imagemUrl}`,
    };
  }
}

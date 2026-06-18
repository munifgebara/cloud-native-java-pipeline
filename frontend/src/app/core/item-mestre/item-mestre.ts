import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface MainItemSummary {
  id: string;
  name: string;
  description: string | null;
  categoryId: string | null;
  categoryName: string | null;
  categoryIcon: string | null;
  imageUrl: string | null;
  active: boolean;
}

export interface SemanticSearchInstance {
  id: string;
  identifier: string | null;
  assetTag: string | null;
  serialNumber: string | null;
  operationalStatus: string;
  currentLocationId: string | null;
  currentLocationName: string | null;
}

export interface SemanticSearchLocation {
  id: string;
  name: string;
  quantidade: number;
}

export interface SemanticSearchItem {
  mainItemId: string;
  name: string;
  description: string | null;
  categoryName: string | null;
  categoryIcon: string | null;
  imageUrl: string | null;
  similaridade: number;
  instancias: SemanticSearchInstance[];
  probableLocations: SemanticSearchLocation[];
}

export interface MainItemResponse {
  id: string;
  name: string;
  description: string | null;
  notes: string | null;
  origemCadastro: string | null;
  categoryId: string | null;
  categoryName: string | null;
  categoryIcon: string | null;
  imageUrl: string | null;
  imagemContentType: string | null;
  imagemTamanhoBytes: number | null;
  imagemGeneratedByAi: boolean;
  imagemProvider: string | null;
  active: boolean;
}

export interface MainItemCreateRequest {
  name: string;
  description?: string | null;
  notes?: string | null;
  origemCadastro?: string | null;
  categoryId?: string | null;
  active?: boolean | null;
}

export interface MainItemUpdateRequest {
  name: string;
  description?: string | null;
  notes?: string | null;
  origemCadastro?: string | null;
  categoryId?: string | null;
  active?: boolean | null;
}

export interface ImageAiRequest {
  name: string;
  categoria?: string | null;
  description?: string | null;
}

export interface ImageAiResponse {
  dataUrl: string;
  contentType: string;
  provider: string;
}

@Injectable({
  providedIn: 'root',
})
export class MainItemService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/main-items`;

  listar(): Observable<MainItemSummary[]> {
    return this.http.get<MainItemSummary[]>(this.baseUrl).pipe(
      map((itens) => itens.map((item) => this.withAbsoluteImageUrl(item)))
    );
  }

  buscarPorNome(name: string): Observable<MainItemSummary[]> {
    return this.http.get<MainItemSummary[]>(`${this.baseUrl}/search`, {
      params: { name },
    }).pipe(map((itens) => itens.map((item) => this.withAbsoluteImageUrl(item))));
  }

  filtrar(name?: string | null, categoryId?: string | null): Observable<MainItemSummary[]> {
    const params: Record<string, string> = {};
    const nomeTratado = (name ?? '').trim();

    if (nomeTratado) {
      params['name'] = nomeTratado;
    }

    if (categoryId) {
      params['categoryId'] = categoryId;
    }

    return this.http.get<MainItemSummary[]>(`${this.baseUrl}/filter`, { params }).pipe(
      map((itens) => itens.map((item) => this.withAbsoluteImageUrl(item)))
    );
  }

  buscarSemanticamente(query: string): Observable<SemanticSearchItem[]> {
    return this.http.get<SemanticSearchItem[]>(`${this.baseUrl}/semantic-search`, {
      params: { query },
    }).pipe(map((itens) => itens.map((item) => this.withAbsoluteImageUrl(item))));
  }

  buscarPorId(id: string): Observable<MainItemResponse> {
    return this.http.get<MainItemResponse>(`${this.baseUrl}/${id}`).pipe(
      map((item) => this.withAbsoluteImageUrl(item))
    );
  }

  criar(payload: MainItemCreateRequest): Observable<MainItemResponse> {
    return this.http.post<MainItemResponse>(this.baseUrl, payload).pipe(
      map((item) => this.withAbsoluteImageUrl(item))
    );
  }

  update(id: string, payload: MainItemUpdateRequest): Observable<MainItemResponse> {
    return this.http.put<MainItemResponse>(`${this.baseUrl}/${id}`, payload).pipe(
      map((item) => this.withAbsoluteImageUrl(item))
    );
  }

  gerarImageIa(payload: ImageAiRequest): Observable<ImageAiResponse> {
    return this.http.post<ImageAiResponse>(`${this.baseUrl}/image-ai`, payload);
  }

  atualizarImagePrincipal(id: string, arquivo: File, generatedByAi = false, provider?: string | null): Observable<MainItemResponse> {
    const formData = new FormData();
    formData.append('arquivo', arquivo);
    formData.append('generatedByAi', String(generatedByAi));
    if (provider) {
      formData.append('provider', provider);
    }

    return this.http.post<MainItemResponse>(`${this.baseUrl}/${id}/main-image`, formData).pipe(
      map((item) => this.withAbsoluteImageUrl(item))
    );
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  private withAbsoluteImageUrl<T extends { imageUrl: string | null }>(item: T): T {
    if (!item.imageUrl || item.imageUrl.startsWith('http')) {
      return item;
    }

    return {
      ...item,
      imageUrl: `${environment.apiBaseUrl}${item.imageUrl}`,
    };
  }
}

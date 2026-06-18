import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface ItemMestreResumo {
  id: string;
  name: string;
  description: string | null;
  categoryId: string | null;
  categoryName: string | null;
  categoryIcon: string | null;
  imageUrl: string | null;
  active: boolean;
}

export interface ConsultaSemanticaInstancia {
  id: string;
  identifier: string | null;
  assetTag: string | null;
  serialNumber: string | null;
  operationalStatus: string;
  currentLocationId: string | null;
  currentLocationName: string | null;
}

export interface ConsultaSemanticaLocal {
  id: string;
  name: string;
  quantidade: number;
}

export interface ConsultaSemanticaItem {
  mainItemId: string;
  name: string;
  description: string | null;
  categoryName: string | null;
  categoryIcon: string | null;
  imageUrl: string | null;
  similaridade: number;
  instancias: ConsultaSemanticaInstancia[];
  probableLocations: ConsultaSemanticaLocal[];
}

export interface ItemMestreResponse {
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

export interface ItemMestreCreateRequest {
  name: string;
  description?: string | null;
  notes?: string | null;
  origemCadastro?: string | null;
  categoryId?: string | null;
  active?: boolean | null;
}

export interface ItemMestreUpdateRequest {
  name: string;
  description?: string | null;
  notes?: string | null;
  origemCadastro?: string | null;
  categoryId?: string | null;
  active?: boolean | null;
}

export interface ImagemIaRequest {
  name: string;
  categoria?: string | null;
  description?: string | null;
}

export interface ImagemIaResponse {
  dataUrl: string;
  contentType: string;
  provider: string;
}

@Injectable({
  providedIn: 'root',
})
export class ItemMestreService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/main-items`;

  listar(): Observable<ItemMestreResumo[]> {
    return this.http.get<ItemMestreResumo[]>(this.baseUrl).pipe(
      map((itens) => itens.map((item) => this.withAbsoluteImageUrl(item)))
    );
  }

  buscarPorNome(name: string): Observable<ItemMestreResumo[]> {
    return this.http.get<ItemMestreResumo[]>(`${this.baseUrl}/search`, {
      params: { name },
    }).pipe(map((itens) => itens.map((item) => this.withAbsoluteImageUrl(item))));
  }

  filtrar(name?: string | null, categoryId?: string | null): Observable<ItemMestreResumo[]> {
    const params: Record<string, string> = {};
    const nomeTratado = (name ?? '').trim();

    if (nomeTratado) {
      params['name'] = nomeTratado;
    }

    if (categoryId) {
      params['categoryId'] = categoryId;
    }

    return this.http.get<ItemMestreResumo[]>(`${this.baseUrl}/filter`, { params }).pipe(
      map((itens) => itens.map((item) => this.withAbsoluteImageUrl(item)))
    );
  }

  buscarSemanticamente(query: string): Observable<ConsultaSemanticaItem[]> {
    return this.http.get<ConsultaSemanticaItem[]>(`${this.baseUrl}/semantic-search`, {
      params: { query },
    }).pipe(map((itens) => itens.map((item) => this.withAbsoluteImageUrl(item))));
  }

  buscarPorId(id: string): Observable<ItemMestreResponse> {
    return this.http.get<ItemMestreResponse>(`${this.baseUrl}/${id}`).pipe(
      map((item) => this.withAbsoluteImageUrl(item))
    );
  }

  criar(payload: ItemMestreCreateRequest): Observable<ItemMestreResponse> {
    return this.http.post<ItemMestreResponse>(this.baseUrl, payload).pipe(
      map((item) => this.withAbsoluteImageUrl(item))
    );
  }

  atualizar(id: string, payload: ItemMestreUpdateRequest): Observable<ItemMestreResponse> {
    return this.http.put<ItemMestreResponse>(`${this.baseUrl}/${id}`, payload).pipe(
      map((item) => this.withAbsoluteImageUrl(item))
    );
  }

  gerarImagemIa(payload: ImagemIaRequest): Observable<ImagemIaResponse> {
    return this.http.post<ImagemIaResponse>(`${this.baseUrl}/image-ai`, payload);
  }

  atualizarImagemPrincipal(id: string, arquivo: File, generatedByAi = false, provider?: string | null): Observable<ItemMestreResponse> {
    const formData = new FormData();
    formData.append('arquivo', arquivo);
    formData.append('generatedByAi', String(generatedByAi));
    if (provider) {
      formData.append('provider', provider);
    }

    return this.http.post<ItemMestreResponse>(`${this.baseUrl}/${id}/main-image`, formData).pipe(
      map((item) => this.withAbsoluteImageUrl(item))
    );
  }

  excluir(id: string): Observable<void> {
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

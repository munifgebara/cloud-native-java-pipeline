import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface ItemMestreResumo {
  id: string;
  nome: string;
  descricao: string | null;
  categoriaId: string | null;
  categoriaNome: string | null;
  categoriaIcone: string | null;
  imagemUrl: string | null;
  ativa: boolean;
}

export interface ConsultaSemanticaInstancia {
  id: string;
  identificador: string | null;
  patrimonio: string | null;
  numeroSerie: string | null;
  statusOperacional: string;
  localAtualId: string | null;
  localAtualNome: string | null;
}

export interface ConsultaSemanticaLocal {
  id: string;
  nome: string;
  quantidade: number;
}

export interface ConsultaSemanticaItem {
  itemMestreId: string;
  nome: string;
  descricao: string | null;
  categoriaNome: string | null;
  categoriaIcone: string | null;
  imagemUrl: string | null;
  similaridade: number;
  instancias: ConsultaSemanticaInstancia[];
  locaisProvaveis: ConsultaSemanticaLocal[];
}

export interface ItemMestreResponse {
  id: string;
  nome: string;
  descricao: string | null;
  observacoes: string | null;
  origemCadastro: string | null;
  categoriaId: string | null;
  categoriaNome: string | null;
  categoriaIcone: string | null;
  imagemUrl: string | null;
  imagemContentType: string | null;
  imagemTamanhoBytes: number | null;
  imagemGeneratedByAi: boolean;
  imagemProvider: string | null;
  ativa: boolean;
}

export interface ItemMestreCreateRequest {
  nome: string;
  descricao?: string | null;
  observacoes?: string | null;
  origemCadastro?: string | null;
  categoriaId?: string | null;
  ativa?: boolean | null;
}

export interface ItemMestreUpdateRequest {
  nome: string;
  descricao?: string | null;
  observacoes?: string | null;
  origemCadastro?: string | null;
  categoriaId?: string | null;
  ativa?: boolean | null;
}

export interface ImagemIaRequest {
  nome: string;
  categoria?: string | null;
  descricao?: string | null;
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
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/itens-mestre`;

  listar(): Observable<ItemMestreResumo[]> {
    return this.http.get<ItemMestreResumo[]>(this.baseUrl).pipe(
      map((itens) => itens.map((item) => this.withAbsoluteImageUrl(item)))
    );
  }

  buscarPorNome(nome: string): Observable<ItemMestreResumo[]> {
    return this.http.get<ItemMestreResumo[]>(`${this.baseUrl}/buscar`, {
      params: { nome },
    }).pipe(map((itens) => itens.map((item) => this.withAbsoluteImageUrl(item))));
  }

  filtrar(nome?: string | null, categoriaId?: string | null): Observable<ItemMestreResumo[]> {
    const params: Record<string, string> = {};
    const nomeTratado = (nome ?? '').trim();

    if (nomeTratado) {
      params['nome'] = nomeTratado;
    }

    if (categoriaId) {
      params['categoriaId'] = categoriaId;
    }

    return this.http.get<ItemMestreResumo[]>(`${this.baseUrl}/filtrar`, { params }).pipe(
      map((itens) => itens.map((item) => this.withAbsoluteImageUrl(item)))
    );
  }

  buscarSemanticamente(consulta: string): Observable<ConsultaSemanticaItem[]> {
    return this.http.get<ConsultaSemanticaItem[]>(`${this.baseUrl}/busca-semantica`, {
      params: { consulta },
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
    return this.http.post<ImagemIaResponse>(`${this.baseUrl}/imagem-ia`, payload);
  }

  atualizarImagemPrincipal(id: string, arquivo: File, generatedByAi = false, provider?: string | null): Observable<ItemMestreResponse> {
    const formData = new FormData();
    formData.append('arquivo', arquivo);
    formData.append('generatedByAi', String(generatedByAi));
    if (provider) {
      formData.append('provider', provider);
    }

    return this.http.post<ItemMestreResponse>(`${this.baseUrl}/${id}/imagem-principal`, formData).pipe(
      map((item) => this.withAbsoluteImageUrl(item))
    );
  }

  excluir(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  private withAbsoluteImageUrl<T extends { imagemUrl: string | null }>(item: T): T {
    if (!item.imagemUrl || item.imagemUrl.startsWith('http')) {
      return item;
    }

    return {
      ...item,
      imagemUrl: `${environment.apiBaseUrl}${item.imagemUrl}`,
    };
  }
}
